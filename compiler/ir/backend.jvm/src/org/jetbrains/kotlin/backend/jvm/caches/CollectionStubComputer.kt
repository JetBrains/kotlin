/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.caches

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.irIntercepted_ConcurrentHashMap
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.isStrictSubtypeOfClass
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.parentAsClass

class CollectionStubComputer(val context: JvmBackendContext) {
    private class LazyStubsForCollectionClass(
        override val readOnlyClass: IrClassSymbol,
        override val mutableClass: IrClassSymbol
    ) : StubsForCollectionClass {
        override val candidatesForStubs: Collection<IrSimpleFunction> by lazy {
            // Old back-end generates stubs for 'class A : C', where
            //  'C' is some "read-only collection" interface from kotlin.collections,
            //  'MC' is a corresponding "mutable collection" interface from kotlin.collections,
            // by building fake overrides for a special 'class X : A(), MC'
            // and taking fake overrides that override members from 'MC'.
            //
            // Here we are looking at this problem from a slightly different angle:
            // we select suitable member functions 'f' in 'MC' that might potentially require stubs (we are here!),
            // and then we generate stubs for functions 'f' that are not effectively overridden by members of 'A'
            // (this happens in the lowering itself).
            //
            // In order for this to be equivalent to the old back-end approach,
            // we should take 'f' in 'MC' such that any of the following conditions is true:
            //  - 'f' is declared in 'MC' - that is, 'f' itself is not a fake override;
            //  - 'f' is abstract and doesn't override anything from 'C'.
            //
            // NB1 it also covers default methods from JDK collection classes case
            // (since that's the only way a member function in 'MC' might be non-abstract).
            //
            // NB2 the scheme of stub method generation in the old back-end depends too much on
            // which particular declarations are present in 'MC'.
            // Some of these declarations are redundant from the stub generation point of view -
            // for example, 'kotlin.collections.MutableListIterator' contains the following (redundant) declarations:
            //      override fun next(): T
            //      override fun hasNext(): Boolean
            // which cause stubs for 'next' and 'hasNext' to be generated in a 'abstract class A<T> : ListIterator<T>'.
            // See https://youtrack.jetbrains.com/issue/KT-36724.
            // In the ideal world, it should be enough to check that
            // the given member function 'f' from 'MC' doesn't override anything from 'C'.

            mutableClass.owner.functions
                .filter { memberFun ->
                    !memberFun.isFakeOverride ||
                            (memberFun.modality == Modality.ABSTRACT && memberFun.overriddenSymbols.none { overriddenFun ->
                                overriddenFun.owner.parentAsClass.symbol == readOnlyClass
                            })
                }
                .toList()
        }
    }

    private val preComputedStubs: Collection<StubsForCollectionClass> by lazy {
        with(context.ir.symbols) {
            listOf(
                LazyStubsForCollectionClass(collection, mutableCollection),
                LazyStubsForCollectionClass(set, mutableSet),
                LazyStubsForCollectionClass(list, mutableList),
                LazyStubsForCollectionClass(map, mutableMap),
                LazyStubsForCollectionClass(mapEntry, mutableMapEntry),
                LazyStubsForCollectionClass(iterable, mutableIterable),
                LazyStubsForCollectionClass(iterator, mutableIterator),
                LazyStubsForCollectionClass(listIterator, mutableListIterator)
            )
        }
    }

    private val stubsCache = irIntercepted_ConcurrentHashMap<IrClass, List<StubsForCollectionClass>>()

    fun stubsForCollectionClasses(irClass: IrClass): List<StubsForCollectionClass> =
        stubsCache.getOrPut(irClass) {
            computeStubsForCollectionClasses(irClass)
        }

    private fun computeStubsForCollectionClasses(irClass: IrClass): List<StubsForCollectionClass> {
        if (irClass.isFromJava()) return emptyList()
        val stubs = preComputedStubs.filter {
            irClass.symbol.isStrictSubtypeOfClass(it.readOnlyClass) && !irClass.symbol.isSubtypeOfClass(it.mutableClass)
        }
        return stubs.filter {
            stubs.none { other -> it.readOnlyClass != other.readOnlyClass && other.readOnlyClass.isSubtypeOfClass(it.readOnlyClass) }
        }
    }
}

interface StubsForCollectionClass {
    val readOnlyClass: IrClassSymbol
    val mutableClass: IrClassSymbol
    val candidatesForStubs: Collection<IrSimpleFunction>
}

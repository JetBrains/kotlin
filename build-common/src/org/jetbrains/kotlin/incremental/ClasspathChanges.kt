/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.FqName
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Changes to the classpath of the `KotlinCompile` task, used to compute the source files that need to be recompiled during an incremental
 * run.
 */
sealed class ClasspathChanges : Serializable {

    class Available() : ClasspathChanges() {

        lateinit var lookupSymbols: List<LookupSymbol>
            private set

        lateinit var fqNames: List<FqName>
            private set

        constructor(lookupSymbols: List<LookupSymbol>, fqNames: List<FqName>) : this() {
            this.lookupSymbols = lookupSymbols
            this.fqNames = fqNames
        }

        private fun writeObject(out: ObjectOutputStream) {
            out.writeInt(lookupSymbols.size)
            lookupSymbols.forEach {
                out.writeUTF(it.name)
                out.writeUTF(it.scope)
            }

            out.writeInt(fqNames.size)
            fqNames.forEach {
                out.writeUTF(it.asString())
            }
        }

        private fun readObject(ois: ObjectInputStream) {
            val lookupSymbolsSize = ois.readInt()
            val lookupSymbols = ArrayList<LookupSymbol>(lookupSymbolsSize)
            repeat(lookupSymbolsSize) {
                val name = ois.readUTF()
                val scope = ois.readUTF()
                lookupSymbols.add(LookupSymbol(name, scope))
            }
            this.lookupSymbols = lookupSymbols

            val fqNamesSize = ois.readInt()
            val fqNames = ArrayList<FqName>(fqNamesSize)
            repeat(fqNamesSize) {
                val fqNameString = ois.readUTF()
                fqNames.add(FqName(fqNameString))
            }
            this.fqNames = fqNames
        }

        companion object {
            private const val serialVersionUID = 0L
        }
    }

    sealed class NotAvailable : ClasspathChanges() {
        object UnableToCompute : NotAvailable()
        object ForNonIncrementalRun : NotAvailable()
        object ClasspathSnapshotIsDisabled : NotAvailable()
        object ReservedForTestsOnly : NotAvailable()
        object ForJSCompiler : NotAvailable()
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.storage.FqNameExternalizer
import org.jetbrains.kotlin.incremental.storage.LinkedHashSetExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolExternalizer
import org.jetbrains.kotlin.name.FqName
import java.io.*

/**
 * Changes to the classpath of the `KotlinCompile` task, used to compute the source files that need to be recompiled during an incremental
 * run.
 */
sealed class ClasspathChanges : Serializable {

    class Available() : ClasspathChanges() {

        companion object {
            private const val serialVersionUID = 0L
        }

        lateinit var lookupSymbols: LinkedHashSet<LookupSymbol>
            private set

        lateinit var fqNames: LinkedHashSet<FqName>
            private set

        constructor(lookupSymbols: LinkedHashSet<LookupSymbol>, fqNames: LinkedHashSet<FqName>) : this() {
            this.lookupSymbols = lookupSymbols
            this.fqNames = fqNames
        }

        private fun writeObject(output: ObjectOutputStream) {
            // Can't close DataOutputStream below as it will also close the underlying ObjectOutputStream, which is still in use.
            ClasspathChangesAvailableExternalizer.save(DataOutputStream(output), this)
        }

        private fun readObject(input: ObjectInputStream) {
            // Can't close DataInputStream below as it will also close the underlying ObjectInputStream, which is still in use.
            ClasspathChangesAvailableExternalizer.read(DataInputStream(input)).also {
                lookupSymbols = it.lookupSymbols
                fqNames = it.fqNames
            }
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

private object ClasspathChangesAvailableExternalizer : DataExternalizer<ClasspathChanges.Available> {

    override fun save(output: DataOutput, classpathChanges: ClasspathChanges.Available) {
        LinkedHashSetExternalizer(LookupSymbolExternalizer).save(output, classpathChanges.lookupSymbols)
        LinkedHashSetExternalizer(FqNameExternalizer).save(output, classpathChanges.fqNames)
    }

    override fun read(input: DataInput): ClasspathChanges.Available {
        return ClasspathChanges.Available(
            lookupSymbols = LinkedHashSetExternalizer(LookupSymbolExternalizer).read(input),
            fqNames = LinkedHashSetExternalizer(FqNameExternalizer).read(input)
        )
    }
}

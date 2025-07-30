/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinFileStubKind
import org.jetbrains.kotlin.psi.stubs.StubUtils.readFqName
import kotlin.reflect.KProperty1

@KtImplementationDetail
internal sealed class KotlinFileStubKindImpl {
    data class File(override val packageFqName: FqName) : KotlinFileStubKindImpl(), KotlinFileStubKind.WithPackage.File {
        override fun toString(): String = toStringGenerator(File::packageFqName)
    }

    data class Script(override val packageFqName: FqName) : KotlinFileStubKindImpl(), KotlinFileStubKind.WithPackage.Script {
        override fun toString(): String = toStringGenerator(Script::packageFqName)
    }

    data class Facade(
        override val packageFqName: FqName,
        override val facadeFqName: FqName,
    ) : KotlinFileStubKindImpl(), KotlinFileStubKind.WithPackage.Facade.Simple {
        override val partSimpleName: String
            get() = facadeFqName.shortName().asString()

        override fun toString(): String = toStringGenerator(Facade::packageFqName, Facade::facadeFqName)

    }

    data class MultifileClass(
        override val packageFqName: FqName,
        override val facadeFqName: FqName,
        override val facadePartSimpleNames: List<String>,
    ) : KotlinFileStubKindImpl(), KotlinFileStubKind.WithPackage.Facade.MultifileClass {
        override fun toString(): String = toStringGenerator(
            MultifileClass::packageFqName,
            MultifileClass::facadeFqName,
            MultifileClass::facadePartSimpleNames,
        )
    }

    companion object {
        fun serialize(kind: KotlinFileStubKind, dataStream: StubOutputStream) {
            kind as KotlinFileStubKindImpl
            when (kind) {
                is File -> {
                    dataStream.writeByte(0)
                    dataStream.writeName(kind.packageFqName.asString())
                }

                is Script -> {
                    dataStream.writeByte(1)
                    dataStream.writeName(kind.packageFqName.asString())
                }

                is Facade -> {
                    dataStream.writeByte(2)
                    dataStream.writeName(kind.packageFqName.asString())
                    dataStream.writeName(kind.facadeFqName.asString())
                }

                is MultifileClass -> {
                    dataStream.writeByte(3)
                    dataStream.writeName(kind.packageFqName.asString())
                    dataStream.writeName(kind.facadeFqName.asString())
                    dataStream.writeVarInt(kind.facadePartSimpleNames.size)
                    kind.facadePartSimpleNames.forEach(dataStream::writeName)
                }
            }
        }

        fun deserialize(dataStream: StubInputStream): KotlinFileStubKind = when (val kind = dataStream.readByte().toInt()) {
            0 -> {
                val packageFqName = dataStream.readFqName()
                File(packageFqName = packageFqName)
            }

            1 -> {
                val packageFqName = dataStream.readFqName()
                Script(packageFqName = packageFqName)
            }

            2 -> {
                val packageFqName = dataStream.readFqName()
                val facadeFqName = dataStream.readFqName()
                Facade(packageFqName = packageFqName, facadeFqName = facadeFqName)
            }

            3 -> {
                val packageFqName = dataStream.readFqName()
                val facadeFqName = dataStream.readFqName()
                val size = dataStream.readVarInt()
                val facadePartSimpleNames = List(size) { dataStream.readNameString()!! }
                MultifileClass(
                    packageFqName = packageFqName,
                    facadeFqName = facadeFqName,
                    facadePartSimpleNames = facadePartSimpleNames,
                )
            }

            else -> error("Unknown file stub kind: $kind")
        }
    }
}

private fun <T : KotlinFileStubKind> T.toStringGenerator(vararg property: KProperty1<T, Any>): String {
    return property.joinToString(prefix = "${this::class.simpleName}[", postfix = "]", separator = ", ") {
        "${it.name}=${it.get(this)}"
    }
}

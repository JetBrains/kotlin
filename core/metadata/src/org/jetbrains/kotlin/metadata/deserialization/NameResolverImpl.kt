/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.QualifiedNameTable.QualifiedName
import java.util.*

class NameResolverImpl(
    val strings: ProtoBuf.StringTable,
    val qualifiedNames: ProtoBuf.QualifiedNameTable
) : NameResolver {
    override fun getString(index: Int): String = strings.getString(index)

    override fun getQualifiedClassName(index: Int): String {
        val (packageFqNameSegments, relativeClassNameSegments) = traverseIds(index)
        val className = relativeClassNameSegments.joinToString(".")
        return if (packageFqNameSegments.isEmpty()) className
        else packageFqNameSegments.joinToString("/") + "/$className"
    }

    override fun isLocalClassName(index: Int): Boolean =
        traverseIds(index).third

    fun getPackageFqName(index: Int): String =
        traverseIds(index).first.joinToString(".")

    private fun traverseIds(startingIndex: Int): Triple<List<String>, List<String>, Boolean> {
        var index = startingIndex
        val packageNameSegments = LinkedList<String>()
        val relativeClassNameSegments = LinkedList<String>()
        var local = false

        while (index != -1) {
            val proto = qualifiedNames.getQualifiedName(index)
            val shortName = strings.getString(proto.shortName)
            when (proto.kind!!) {
                QualifiedName.Kind.CLASS -> relativeClassNameSegments.addFirst(shortName)
                QualifiedName.Kind.PACKAGE -> packageNameSegments.addFirst(shortName)
                QualifiedName.Kind.LOCAL -> {
                    relativeClassNameSegments.addFirst(shortName)
                    local = true
                }
            }

            index = proto.parentQualifiedName
        }
        return Triple(packageNameSegments, relativeClassNameSegments, local)
    }
}

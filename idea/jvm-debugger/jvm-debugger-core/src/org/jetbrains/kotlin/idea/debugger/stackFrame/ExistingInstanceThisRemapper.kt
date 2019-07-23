/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stackFrame

import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueChildrenList
import org.jetbrains.kotlin.utils.getSafe
import java.lang.reflect.Modifier

// Very Dirty Work-around.
// We should stop delegating to the Java stack frame and generate our trace elements from scratch.
class ExistingInstanceThisRemapper(
    private val children: XValueChildrenList,
    private val index: Int,
    val value: XValue,
    private val size: Int
) {
    companion object {
        private val LOG = Logger.getInstance(this::class.java)

        private const val THIS_NAME = "this"

        fun find(children: XValueChildrenList): ExistingInstanceThisRemapper? {
            val size = children.size()
            for (i in 0 until size) {
                if (children.getName(i) == THIS_NAME) {
                    val valueDescriptor = (children.getValue(i) as? JavaValue)?.descriptor
                    @Suppress("FoldInitializerAndIfToElvis")
                    if (valueDescriptor !is ThisDescriptorImpl) {
                        return null
                    }

                    return ExistingInstanceThisRemapper(
                        children,
                        i,
                        children.getValue(i),
                        size
                    )
                }
            }

            return null
        }
    }

    fun remapName(newName: String) {
        val (names, _) = getLists() ?: return
        names[index] = newName
    }

    fun remove() {
        val (names, values) = getLists() ?: return
        names.removeAt(index)
        values.removeAt(index)
    }

    private fun getLists(): Lists? {
        if (children.size() != size) {
            throw IllegalStateException("Children list was modified")
        }

        var namesList: MutableList<Any?>? = null
        var valuesList: MutableList<Any?>? = null

        for (field in XValueChildrenList::class.java.declaredFields) {
            val mods = field.modifiers
            if (Modifier.isPrivate(mods) && Modifier.isFinal(mods) && !Modifier.isStatic(mods) && field.type == List::class.java) {
                @Suppress("UNCHECKED_CAST")
                val list = (field.getSafe(children) as? MutableList<Any?>)?.takeIf { it.size == size } ?: continue

                if (list[index] == THIS_NAME) {
                    namesList = list
                } else if (list[index] === value) {
                    valuesList = list
                }
            }

            if (namesList != null && valuesList != null) {
                return Lists(namesList, valuesList)
            }
        }

        LOG.error(
            "Can't find name/value lists, existing fields: "
                    + XValueChildrenList::class.java.declaredFields?.contentToString()
        )

        return null
    }

    private data class Lists(val names: MutableList<Any?>, val values: MutableList<Any?>)
}
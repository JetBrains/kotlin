/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

import konan.internal.ExportForCppRuntime

// TODO: it it actually not related to cinterop.

@ExportTypeInfo("theNSArrayListTypeInfo")
internal class NSArrayList : AbstractList<Any?> {

    // FIXME: override methods of Any.

    @konan.internal.ExportForCppRuntime("Kotlin_NSArrayList_constructor")
    constructor() : super()

    override val size: Int get() = getSize()

    @SymbolName("Kotlin_NSArrayList_getSize")
    private external fun getSize(): Int

    @SymbolName("Kotlin_NSArrayList_getElement")
    external override fun get(index: Int): Any?
}

@ExportForCppRuntime private fun Kotlin_List_get(list: List<*>, index: Int): Any? = list.get(index)
@ExportForCppRuntime private fun Kotlin_List_getSize(list: List<*>): Int = list.size

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.analysis

import androidx.compose.plugins.kotlin.AbstractComposeDiagnosticsTest

class UnionCheckerTests : AbstractComposeDiagnosticsTest() {

    fun testUnionTypeReporting001() {
        doTest(
            """
            import androidx.compose.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar() {
                <foo value=1 />
                <foo value="1" />
                <foo value=<!ILLEGAL_ASSIGN_TO_UNIONTYPE!>1f<!> />
            }
        """)
    }

    fun testUnionTypeReporting002() {
        doTest(
            """
            import androidx.compose.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class) Any) {
                <foo value />
            }
        """)
    }

    fun testUnionTypeReporting003() {
        doTest(
            """
            import androidx.compose.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class, Float::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class) Any) {
                <foo value />
            }
        """)
    }

    fun testUnionTypeReporting004() {
        doTest(
            """
            import androidx.compose.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class, Float::class) Any) {
                <foo <!ILLEGAL_ASSIGN_TO_UNIONTYPE!>value<!> />
            }
        """)
    }
}

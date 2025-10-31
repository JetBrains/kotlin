/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by DevKit IDE Plugin to find "related testData".
 * Plugin provides various IDE-assistance (e.g. "NavigateToTestData"-actions and gutter icons).
 *
 * The main annotation is @[com.intellij.testFramework.TestDataPath].
 * @[TestDataPath] is usually set on the base class, and @[TestMetadata] - on test methods.
 * Without @[TestMetadata], a path based on test name is computed:
 *
 * <table summary="">
 * <tr> <th>Lookup rule</th>            <th>Test name</th>  <th>Related testData path</th> </tr>
 * <tr> <td>default</td>                <td>testFoo</td>    <td>'{argument-of-@TestDataPath}/foo'</td> </tr>
 * <tr> <td>with @[TestMetadata]</td>   <td>testFoo</td>    <td>'{argument-of-@TestDataPath}/{argument-of-@TestMetadata}'</td> </tr>
 * </table>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TestMetadata {
    String value();
}

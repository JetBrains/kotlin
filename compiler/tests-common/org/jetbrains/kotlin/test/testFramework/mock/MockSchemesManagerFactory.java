/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.test.testFramework.mock;

import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.options.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockSchemesManagerFactory extends SchemesManagerFactory {
  private static final SchemesManager EMPTY = new EmptySchemesManager();

  @Override
  protected <T extends Scheme, E extends ExternalizableScheme> SchemesManager<T, E> create(@NotNull String directoryName,
                                                                                           @NotNull SchemeProcessor<E> processor,
                                                                                           @NotNull RoamingType roamingType,
                                                                                           @Nullable String presentableName) {
    //noinspection unchecked
    return EMPTY;
  }
}

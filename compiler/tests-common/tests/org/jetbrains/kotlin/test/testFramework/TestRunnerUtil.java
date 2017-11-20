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
package org.jetbrains.kotlin.test.testFramework;

import org.jetbrains.annotations.TestOnly;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class TestRunnerUtil {
  private TestRunnerUtil() {
  }

  @TestOnly
  public static boolean isJUnit4TestClass(Class aClass) {
    int modifiers = aClass.getModifiers();
    if ((modifiers & Modifier.ABSTRACT) != 0) return false;
    if ((modifiers & Modifier.PUBLIC) == 0) return false;
    if (aClass.getAnnotation(RunWith.class) != null) return true;
    for (Method method : aClass.getMethods()) {
      if (method.getAnnotation(Test.class) != null) return true;
    }
    return false;
  }
}

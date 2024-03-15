/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.java.decompiler.main.extern;

import java.util.Map;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;

public interface IVariableNameProvider {
  public Map<VarVersionPair,String> rename(Map<VarVersionPair,String> variables);
  default String renameAbstractParameter(String name, int index) {
    return name;
  }

  default String renameParameter(int flags, String type, String name, int index) {
    if ((flags & (CodeConstants.ACC_ABSTRACT | CodeConstants.ACC_NATIVE)) != 0) {
      return renameAbstractParameter(name, index);
    }

    return name;
  }
  public void addParentContext(IVariableNameProvider renamer);
}

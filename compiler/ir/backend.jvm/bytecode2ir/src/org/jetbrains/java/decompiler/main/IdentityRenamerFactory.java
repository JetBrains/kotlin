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
package org.jetbrains.java.decompiler.main;

import java.util.Map;

import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructMethod;

public class IdentityRenamerFactory implements IVariableNamingFactory, IVariableNameProvider {
  @Override
  public IVariableNameProvider createFactory(StructMethod method) {
    return this;
  }

  @Override
  public String renameAbstractParameter(String abstractParam, int index) {
    return abstractParam;
  }

  @Override
  public Map<VarVersionPair, String> rename(Map<VarVersionPair, String> variables) {
    return null;
  }

  @Override
  public void addParentContext(IVariableNameProvider renamer) {
  }
}

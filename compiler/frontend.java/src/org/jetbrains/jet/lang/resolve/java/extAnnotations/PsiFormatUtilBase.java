/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.extAnnotations;

import com.intellij.openapi.util.text.StringUtil;

/**
 * This class is copied from IDEA.
 * This class should be eliminated when Kotlin will depend on IDEA 12.x (KT-2326)
 * @author Evgeny Gerashchenko
 * @since 6/26/12
 */
@Deprecated
public abstract class PsiFormatUtilBase {

  public static final int SHOW_NAME = 0x0001; // variable, method, class
  public static final int SHOW_TYPE = 0x0002; // variable, method
  public static final int TYPE_AFTER = 0x0004; // variable, method
  public static final int SHOW_MODIFIERS = 0x0008; // variable, method, class
  public static final int MODIFIERS_AFTER = 0x0010; // variable, method, class
  public static final int SHOW_REDUNDANT_MODIFIERS = 0x0020; // variable, method, class, modifier list
  public static final int SHOW_PACKAGE_LOCAL = 0x0040; // variable, method, class, modifier list
  public static final int SHOW_INITIALIZER = 0x0080; // variable
  public static final int SHOW_PARAMETERS = 0x0100; // method
  public static final int SHOW_THROWS = 0x0200; // method
  public static final int SHOW_EXTENDS_IMPLEMENTS = 0x0400; // class
  public static final int SHOW_FQ_NAME = 0x0800; // class, field, method
  public static final int SHOW_CONTAINING_CLASS = 0x1000; // field, method
  public static final int SHOW_FQ_CLASS_NAMES = 0x2000; // variable, method, class
  public static final int JAVADOC_MODIFIERS_ONLY = 0x4000; // field, method, class
  public static final int SHOW_ANONYMOUS_CLASS_VERBOSE = 0x8000; // class
  public static final int SHOW_RAW_TYPE = 0x10000; //type
  public static final int SHOW_RAW_NON_TOP_TYPE = 0x20000;
  public static final int MAX_PARAMS_TO_SHOW = 7;

  protected static void appendSpaceIfNeeded(StringBuilder buffer) {
    if (buffer.length() != 0 && !StringUtil.endsWithChar(buffer, ' ')) {
      buffer.append(' ');
    }
  }
}

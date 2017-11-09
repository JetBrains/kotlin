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

package org.jetbrains.kotlin.android.intention

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile


val CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY = Key<String>("CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY")

class CreateXmlResourceParameters(val name: String,
                                  val value: String,
                                  val fileName: String,
                                  val resourceDirectory: VirtualFile,
                                  val directoryNames: List<String>)
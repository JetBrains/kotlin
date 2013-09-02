/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

/*
* This class indicates whether we should look into kotlin sources when searching for descriptors in JavaDescriptorResolver.
* This is a hack because it should be done via correct scopes.
* Order in which we attempt to resolve descriptors is also should be taken care of. (though it is correct for the most part now)
* */
public enum DescriptorSearchRule {
    //Return immediately if you found descriptor in kotlin sources, if not continue
    INCLUDE_KOTLIN_SOURCES,
    //Do not try to find descriptors in kotlin sources.
    //This flag is mostly used when resolving descriptors from binaries or java descriptors.
    //It will not prevent from looking into java sources which is often desirable behaviour.
    //It is not correct because sometimes class from sources can override class from binary since it comes earlier in classpath
    //and for a thousand more reasons.
    IGNORE_KOTLIN_SOURCES
}

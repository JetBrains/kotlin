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

package org.jetbrains.jet.lang.resolve.name;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Stepan Koltsov
 */
public class NamePredicateUtils {

    public static Collection<DeclarationDescriptor> filter(
                @NotNull Collection<DeclarationDescriptor> descriptors,
                @NotNull NamePredicate predicate) {
        if (predicate.isAll()) {
            return descriptors;
        }
        else {
            return Collections2.filter(descriptors, predicate.asGuavaDescriptorPredicate());
        }
    }

    public static <B>Map<Name, B> filterKeys(
            @NotNull Map<Name, B> map,
            @NotNull NamePredicate predicate) {
        Name exact = predicate.getExact();

        if (predicate.isAll()) {
            return map;
        }
        else if (exact != null) {
            B value = map.get(exact);
            if (value != null) {
                return Collections.singletonMap(exact, value);
            }
            else {
                return Collections.emptyMap();
            }
        }
        else {
            Map<Name, B> r = Maps.newHashMap();
            for (Map.Entry<Name, B> entry : map.entrySet()) {
                if (predicate.matches(entry.getKey())) {
                    r.put(entry.getKey(), entry.getValue());
                }
            }
            return r;
        }
    }
}

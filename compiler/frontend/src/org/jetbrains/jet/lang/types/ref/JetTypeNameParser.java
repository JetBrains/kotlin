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

package org.jetbrains.jet.lang.types.ref;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;
import java.util.regex.Pattern;

class JetTypeNameParser extends SimpleParser {
    public JetTypeNameParser(String input) {
        super(input);
    }


    private static final Pattern namePattern = Pattern.compile("(?i)[a-z][a-z0-9]*");

    @NotNull
    public Name parseName() {
        return Name.identifier(consume(namePattern));
    }

    @NotNull
    public FqName parseFqName() {
        FqName fqName = FqName.topLevel(parseName());
        while (consumeIfLookingAt('.')) {
            fqName = fqName.child(parseName());
        }
        return fqName;
    }

    @NotNull
    public JetTypeName parse() {
        FqName fqName = parseFqName();
        List<JetTypeName> typeArguments = Lists.newArrayList();
        if (consumeIfLookingAt('<')) {
            typeArguments.add(parse());
            while (consumeIfLookingAt(',')) {
                typeArguments.add(parse());
            }
            consume('>');
        }
        return new JetTypeName(fqName, typeArguments);
    }

    @NotNull
    public static JetTypeName parse(@NotNull String string) {
        JetTypeNameParser parser = new JetTypeNameParser(string);
        JetTypeName typeName = parser.parse();
        parser.checkEof();
        return typeName;
    }

}

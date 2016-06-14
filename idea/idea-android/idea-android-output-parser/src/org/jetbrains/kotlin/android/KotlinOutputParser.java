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

package org.jetbrains.kotlin.android;

import com.android.ide.common.blame.parser.PatternAwareOutputParser;
import com.android.ide.common.blame.parser.util.OutputLineReader;
import com.android.utils.ILogger;

import java.util.List;

public class KotlinOutputParser implements PatternAwareOutputParser {
    @Override
    public boolean parse(String s, OutputLineReader reader, List list, ILogger logger) {
        return KotlinOutputParserHelperKt.parse(s, reader, list, logger);
    }
}

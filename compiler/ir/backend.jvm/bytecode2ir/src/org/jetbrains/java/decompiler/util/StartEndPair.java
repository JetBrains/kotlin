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
package org.jetbrains.java.decompiler.util;

public class StartEndPair {
  public final int start;
  public final int end;
  public StartEndPair(int start, int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  public boolean equals(Object obj) {
    return ((StartEndPair)obj).start == start && ((StartEndPair)obj).end == end;
  }

  @Override
  public int hashCode() {
    return start * 31 + end;
  }

  @Override
  public String toString() {
    return String.format("%d->%d",start,end);
  }

  public static StartEndPair join(StartEndPair... pairs) {
    int start = Integer.MAX_VALUE;
    int end = Integer.MIN_VALUE;
    for (StartEndPair pair : pairs) {
        if (pair == null) continue;
        start = Math.min(start, pair.start);
        end = Math.max(end, pair.end);
    }
    return new StartEndPair(start, end);
  }
}

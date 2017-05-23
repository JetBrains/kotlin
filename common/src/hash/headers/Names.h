/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
#ifndef COMMON_NAMES_H
#define COMMON_NAMES_H

#include <stdint.h>

// All names in system are stored as hashes (or maybe, for debug builds,
// as pointers to uniqued C strings containing names?).
// There are two types of hashes:
//  - local hash, must be unique per class/scope (CityHash64 is being used)
//  - global hash, must be unique globally (SHA1 is being used)
// Generic guideline is that global hash is being used in global persistent
// context, while local hashes are more local in scope.
// Local hash.
typedef int64_t LocalHash;
// Hash of field name.
typedef LocalHash FieldNameHash;
// Hash of open method name.
typedef LocalHash MethodNameHash;
// Global hash.
typedef struct {
  uint8_t bits[20];
} GlobalHash;
// Hash of function name.
typedef GlobalHash FunctionNameHash;
// Hash of class name.
typedef GlobalHash ClassNameHash;

#ifdef __cplusplus
extern "C" {
#endif
// Make local hash out of arbitrary data.
void MakeLocalHash(const void* data, uint32_t size, LocalHash* hash);
// Make global hash out of arbitrary data.
void MakeGlobalHash(const void* data, uint32_t size, GlobalHash* hash);
// Make printable C string out of local hash.
void PrintableLocalHash(const LocalHash* hash, char* buffer, uint32_t size);
// Make printable C string out of global hash.
void PrintableGlobalHash(const GlobalHash* hash, char* buffer, uint32_t size);
#ifdef __cplusplus
} // extern "C"
#endif

#endif // COMMON_NAMES_H

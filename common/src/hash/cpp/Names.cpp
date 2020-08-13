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
#include <cassert>

#include "Names.h"

#include "Base64.h"
#include "City.h"
#include "Sha1.h"

namespace {

constexpr uint32_t PrintableHexSize(uint32_t input_length) {
  return input_length * 2;
}

void PrintableHex(const uint8_t* data, uint32_t data_length, char* hex) {
  static const char* hex_digits = "0123456789ABCDEF";
  for (uint32_t i = 0; i < data_length; ++i) {
    *hex++ = hex_digits[(*data >> 4) & 0xf];
    *hex++ = hex_digits[(*data++) & 0xf];
  }
}

constexpr uint32_t PrintableBase64Size(uint32_t input_length) {
  return ((input_length + 2) / 3 * 4) + 1;
}

void PrintableBase64(const uint8_t* data, uint32_t data_length, char* base64) {
  int rv = EncodeBase64(data, data_length, base64, PrintableBase64Size(data_length));
  assert(rv == 0);
}

} // namespace

extern "C" {

// Make local hash out of arbitrary data.
void MakeLocalHash(const void* data, uint32_t size, LocalHash* hash) {
  *hash = CityHash64(data, size);
}

// Make global hash out of arbitrary data.
void MakeGlobalHash(const void* data, uint32_t size, GlobalHash* hash) {
  SHA1_CTX ctx;
  SHA1Init(&ctx);
  SHA1Update(&ctx, reinterpret_cast<const unsigned char *>(data), size);
  SHA1Final(&hash->bits[0], &ctx);
}

// Make printable C string out of local hash.
void PrintableLocalHash(const LocalHash* hash, char* buffer, uint32_t size) {
  if (size < PrintableBase64Size(sizeof(*hash))) {
    assert(false);
    return;
  }
  PrintableBase64(reinterpret_cast<const uint8_t*>(&hash), sizeof(*hash), buffer);
}

// Make printable C string out of global hash.
void PrintableGlobalHash(const GlobalHash* hash, char* buffer, uint32_t size) {
  if (size < PrintableBase64Size(sizeof(*hash))) {
    assert(false);
    return;
  }
  PrintableBase64(hash->bits, sizeof(*hash), buffer);
}

} // extern "C"

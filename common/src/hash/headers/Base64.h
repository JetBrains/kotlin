#ifndef COMMON_BASE64_H
#define COMMON_BASE64_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int Base64Encode(
    const void* input, uint32_t inputLen, void* output, uint32_t outputLen);

int Base64Decode(
    const char* input, uint32_t inputLen, void* output, uint32_t* outputLen);

#ifdef __cplusplus
}
#endif

#endif // COMMON_BASE64_H

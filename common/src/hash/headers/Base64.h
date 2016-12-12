#ifndef COMMON_BASE64_H
#define COMMON_BASE64_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int EncodeBase64(
    const void* input, uint32_t inputLen, void* output, uint32_t outputLen);

int DecodeBase64(
    const char* input, uint32_t inputLen, void* output, uint32_t* outputLen);

#ifdef __cplusplus
}
#endif

#endif // COMMON_BASE64_H

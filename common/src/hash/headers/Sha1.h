#ifndef COMMON_SHA1_H
#define COMMON_SHA1_H

#include <stdint.h>

/*
SHA-1 in C
By Steve Reid <steve@edmweb.com>
100% Public Domain
*/

#ifdef __cplusplus
extern "C" {
#endif

typedef struct SHA1_CTX {
    uint32_t state[5];
    uint32_t count[2];
    unsigned char buffer[64];
} SHA1_CTX;

void SHA1Init(SHA1_CTX* context);
void SHA1Update(SHA1_CTX* context, const unsigned char* data, uint32_t len);
void SHA1Final(unsigned char digest[20], SHA1_CTX* context);

#ifdef __cplusplus
}
#endif

#endif // COMMON_SHA1_H

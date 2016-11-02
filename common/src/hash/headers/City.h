#ifndef COMMON_CITY_H
#define COMMON_CITY_H

// CityHash, by Geoff Pike and Jyrki Alakuijala.

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// Hash function for a byte array.
uint64_t CityHash64(const void* buf, size_t len);

#ifdef __cplusplus
}
#endif

#endif // COMMON_CITY_H

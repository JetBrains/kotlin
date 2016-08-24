
#include <stdint.h>


uint32_t prev_number = 0;

#define MULTIPLIER 1664525
#define INCREMENT 1013904223
#define MODULE UINT32_MAX

uint32_t car_random_get_int(void)
{
    prev_number = (prev_number * MULTIPLIER + INCREMENT) % MODULE;
    return prev_number;
}

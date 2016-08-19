#pragma once

#include <stdbool.h>
#include "stm32f4_discovery.h"

typedef enum
{
    CAR_LED_GREEN = LED4,
    CAR_LED_ORANGE = LED3,
    CAR_LED_RED = LED5,
    CAR_LED_BLUE = LED6
} car_led_t;

void car_leds_init(void);
void car_leds_clear_all(void);
void car_led_set(int led, bool on);

#pragma once

#include <stdbool.h>
#include "stm32f4_discovery.h"

typedef enum
{
    LED_GREEN = LED4,
    LED_ORANGE = LED3,
    LED_RED = LED5,
    LED_BLUE = LED6
} led_t;

void leds_init(void);
void leds_clear_all(void);
void led_set(int led, bool on);

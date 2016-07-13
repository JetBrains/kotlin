#pragma once

#include <stdbool.h>

#include "stm32f4_discovery.h"
#include "stm32f4xx_conf.h"

inline void leds_init(void)
{
    STM_EVAL_LEDInit(LED4);
    STM_EVAL_LEDInit(LED3);
    STM_EVAL_LEDInit(LED5);
    STM_EVAL_LEDInit(LED6);
}

typedef enum
{
    LED_GREEN = LED4,
    LED_ORANGE = LED3,
    LED_RED = LED5,
    LED_BLUE = LED6
} led_t;

inline void led_set(led_t led, bool on)
{
    if (on)
        STM_EVAL_LEDOn(led);
    else
        STM_EVAL_LEDOff(led);
}


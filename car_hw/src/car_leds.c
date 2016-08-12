#include <stdbool.h>

#include "car_leds.h"

#include "stm32f4_discovery.h"
#include "stm32f4xx_conf.h"

void leds_init(void)
{
    STM_EVAL_LEDInit(LED_RED);
    STM_EVAL_LEDInit(LED_ORANGE);
    STM_EVAL_LEDInit(LED_GREEN);
    STM_EVAL_LEDInit(LED_BLUE);
}

void led_set(int led, bool on)
{
    if (on)
        STM_EVAL_LEDOn(led);
    else
        STM_EVAL_LEDOff(led);
}

void leds_clear_all(void)
{
    led_set(LED_GREEN, false);
    led_set(LED_ORANGE, false);
    led_set(LED_RED, false);
    led_set(LED_BLUE, false);
}

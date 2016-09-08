#include <stdbool.h>

#include "car_leds.h"

#include "stm32f4_discovery.h"
#include "stm32f4xx_conf.h"

void car_leds_init(void)
{
    STM_EVAL_LEDInit(CAR_LED_RED);
    STM_EVAL_LEDInit(CAR_LED_ORANGE);
    STM_EVAL_LEDInit(CAR_LED_GREEN);
    STM_EVAL_LEDInit(CAR_LED_BLUE);
}

void car_led_set(int led, bool on)
{
    if (on)
        STM_EVAL_LEDOn(led);
    else
        STM_EVAL_LEDOff(led);
}

void car_leds_clear_all(void)
{
    car_led_set(CAR_LED_GREEN, false);
    car_led_set(CAR_LED_ORANGE, false);
    car_led_set(CAR_LED_RED, false);
    car_led_set(CAR_LED_BLUE, false);
}

#pragma once

#include <stdbool.h>

#include "stm32f4_discovery.h"
#include "stm32f4xx_conf.h"

#define CAR_LED_GPIO_PORT_CLOCK RCC_AHB1Periph_GPIOD
#define CAR_LED_GPIO_PORT GPIOD
#define CAR_LED_PIN_GREEN GPIO_Pin_12
#define CAR_LED_PIN_ORANGE GPIO_Pin_13
#define CAR_LED_PIN_RED GPIO_Pin_14
#define CAR_LED_PIN_BLUE GPIO_Pin_15

inline void leds_init(void)
{
    GPIO_InitTypeDef GPIO_InitStructure;
    RCC_AHB1PeriphClockCmd(CAR_LED_GPIO_PORT_CLOCK, ENABLE);
    GPIO_InitStructure.GPIO_Pin = CAR_LED_PIN_GREEN
        | CAR_LED_PIN_ORANGE
        | CAR_LED_PIN_RED
        | CAR_LED_PIN_BLUE;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_OUT;
    GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_2MHz;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_NOPULL;
    GPIO_Init(CAR_LED_GPIO_PORT, &GPIO_InitStructure);
}

typedef enum
{
    LED_GREEN,
    LED_ORANGE,
    LED_RED,
    LED_BLUE
} led_t;

inline void led_set(led_t led, bool on)
{
    int led_pin = CAR_LED_PIN_GREEN;
    switch(led) {
        case LED_ORANGE:
            led_pin = CAR_LED_PIN_ORANGE;
        break;
        case LED_RED:
            led_pin = CAR_LED_PIN_RED;
        break;
        case LED_BLUE:
            led_pin = CAR_LED_PIN_BLUE;
        break;
    }

    if (on)
        GPIO_SetBits(CAR_LED_GPIO_PORT, led_pin);
    else
        GPIO_ResetBits(CAR_LED_GPIO_PORT, led_pin);
}


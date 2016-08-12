#pragma once

#include <stdbool.h>
#include "stm32f4_discovery.h"

typedef void (*user_btn_handler_t)(void);
static user_btn_handler_t user_btn_handler;

// @handler can be NULL
inline void user_btn_init(user_btn_handler_t handler)
{
    user_btn_handler = handler;
    STM_EVAL_PBInit(BUTTON_USER, BUTTON_MODE_EXTI);
}

inline bool user_btn_is_pushed(void)
{
    return STM_EVAL_PBGetState(BUTTON_USER) == Bit_SET;
}

void EXTI0_IRQHandler(void)
{
    if (user_btn_handler)
        user_btn_handler();
    EXTI_ClearITPendingBit(USER_BUTTON_EXTI_LINE);
}

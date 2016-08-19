#pragma once

#include <stdbool.h>
#include "stm32f4_discovery.h"

typedef void (*car_user_btn_handler_t)(void);

// @handler can be NULL
void car_user_btn_init(car_user_btn_handler_t handler);

bool car_user_btn_is_pushed(void);

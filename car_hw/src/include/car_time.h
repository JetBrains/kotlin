#pragma once

#include <stdint.h>

void car_time_init(void);
void car_time_wait(volatile uint32_t ms);
uint32_t car_time_get_timestamp(void);
/*
 * Called from systick handler
 */
void car_time_irq_handler();

#pragma once

void time_init(void);
void wait(volatile uint32_t ms);
uint32_t get_timestamp(void);
/*
 * Called from systick handler
 */
void timer_irq_handler();

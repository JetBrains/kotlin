#include <stm32f4xx.h>

#include "time.h"

static volatile uint32_t pending_timer_ticks, ticks_since_boot;

void time_init(void)
{
    // XXX this is copy pasted and it doesn't provide 1ms periodic
    // timer interrupt
    if (SysTick_Config(SystemCoreClock / 1000))
        while (1){};
}

void wait(volatile uint32_t ms) {
    pending_timer_ticks = ms;
    while(pending_timer_ticks){};
}

uint32_t get_timestamp(void)
{
    return ticks_since_boot;
}

void timer_irq_handler() {
    if (pending_timer_ticks)
        pending_timer_ticks--;

    ticks_since_boot++;
}

#include <stddef.h>
#include <stdbool.h>
#include <usbd_cdc_vcp.h>

#include "car_rc.h"
#include "car_leds.h"
#include "car_engine.h"


void run_rc_car(volatile bool *stop)
{
    led_set(LED_GREEN, true);
    led_set(LED_ORANGE, true);
    led_set(LED_RED, true);
    led_set(LED_BLUE, true);

    while(!*stop) {

    }
    leds_clear_all();
}

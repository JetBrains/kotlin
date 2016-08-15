#include <stddef.h>
#include <stdbool.h>

#include "car_engine.h"
#include "car_leds.h"
#include "car_rc.h"
#include "car_user_btn.h"
#include "communication.h"
#include "time.h"

#include "stm32f4xx_conf.h"
#include "stm32f4xx_it.h"

int state = 0;

void set_state(int i) {
    state = i;
}

int get_state() {
    return state;
}


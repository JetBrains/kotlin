#include <stddef.h>
#include <stdbool.h>

#include "memory.h"

#include "car_leds.h"
#include "car_engine.h"
#include "time.h"
#include "car_user_btn.h"

extern int kotlin_main();

int cur_mode;
bool cur_stop = false;

int get_mode() {
  return cur_mode;
}

void set_mode(int mode) {
  cur_mode = mode;
}

void stop_current_mode() {
  cur_stop = true;
}

void start_current_mode() {
  cur_stop = false;
}

int main(void)
{
    kotlin_main();
}

#include "car_leds.h"
#include "car_engine.h"
#include "wait.h"

const uint32_t PROGRAM_DURATION = 0x3FFFFF; 
static void program_forward(void)
{
    engine_forward();
    wait(PROGRAM_DURATION);
}

static void program_backward(void)
{
    engine_backward();
    wait(PROGRAM_DURATION);
}

static void program_rotation_left(void)
{
    engine_turn_left();
    wait(PROGRAM_DURATION);
}

static void program_rotation_right(void)
{
    engine_turn_right();
    wait(PROGRAM_DURATION);
}

static void program_arbitraty_route(void)
{
    const int ROUTE_PIECE_DURATION = 6 * PROGRAM_DURATION;
    engine_forward();
    wait(ROUTE_PIECE_DURATION);

    engine_backward();
    wait(ROUTE_PIECE_DURATION);

    engine_forward();
    wait(ROUTE_PIECE_DURATION / 2);

    engine_turn_right();
    wait(ROUTE_PIECE_DURATION);

    engine_forward();
    wait(ROUTE_PIECE_DURATION / 2);

    engine_backward();
    wait(ROUTE_PIECE_DURATION / 2);

    engine_turn_left();
    wait(ROUTE_PIECE_DURATION);

    engine_backward();
    wait(ROUTE_PIECE_DURATION / 2);

    engine_stop();
    wait(ROUTE_PIECE_DURATION / 2);
}


int main(void)
{
    /*!< At this stage the microcontroller clock setting is already configured,
       this is done through SystemInit() function which is called from startup
       file (startup_stm32f4xx.s) before to branch to application main.
       To reconfigure the default setting of SystemInit() function, refer to
       system_stm32f4xx.c file
     */

    leds_init();
    engine_init();

    while(1) {
        program_forward();
    }
}

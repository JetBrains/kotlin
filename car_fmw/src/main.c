#include <stddef.h>

#include "car_leds.h"
#include "car_engine.h"
#include "car_user_btn.h"
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

typedef void (*car_program_code_t)(void);
typedef struct car_program_desc {
    car_program_code_t code;
    struct {
        bool green : 1;
        bool orange : 1;
        bool red : 1;
        bool blue : 1;
    } leds;
} car_program_desc_t;
static car_program_desc_t const PROGRAMS[] = {
    { .code = program_forward, .leds = { .green = 1 } },
    { .code = program_backward, .leds = { .orange = 1 } },
    { .code = program_rotation_left, .leds = { .red = 1 } },
    { .code = program_rotation_right, .leds = { .blue = 1 } },
    { .code = program_arbitraty_route, .leds = { .green = 1, .orange = 1 } }
};
static const size_t PROGRAMS_CNT = sizeof(PROGRAMS) / sizeof(PROGRAMS[0]);
static size_t cur_program_ix;
static __IO bool next_program_pending = false;

static void set_cur_program(size_t program_ix)
{
    cur_program_ix = program_ix;

    led_set(LED_GREEN, false);
    led_set(LED_ORANGE, false);
    led_set(LED_RED, false);
    led_set(LED_BLUE, false);

    const car_program_desc_t *cpd = &PROGRAMS[cur_program_ix];
    if (cpd->leds.green)
        led_set(LED_GREEN, true);
    if (cpd->leds.orange)
        led_set(LED_ORANGE, true);
    if (cpd->leds.red)
        led_set(LED_RED, true);
    if (cpd->leds.blue)
        led_set(LED_BLUE, true);
}

void proc_next_program_pending(void)
{
    if (!next_program_pending)
        return;
    next_program_pending = false;
    set_cur_program((cur_program_ix + 1) % PROGRAMS_CNT);
}

void set_next_program_pending(void)
{
    next_program_pending = true;
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
    user_btn_init(set_next_program_pending);
    set_cur_program(0);

    while(1) {
        proc_next_program_pending();
        PROGRAMS[cur_program_ix].code();
    }
}

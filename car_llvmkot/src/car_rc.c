#include <stddef.h>
#include <stdbool.h>
#include <usbd_cdc_vcp.h>

#include "car_rc.h"
#include "car_leds.h"
#include "car_engine.h"

typedef enum {
    RC_CAR_CMD_STOP = '0',
    RC_CAR_CMD_FWD = '1',
    RC_CAR_CMD_BKWD = '2',
    RC_CAR_CMD_RIGHT = '3',
    RC_CAR_CMD_LEFT = '4'
} RC_CAR_CMD;

void send_cmd_result(uint8_t cmd, uint8_t result)
{
    VCP_put_char('(');
    VCP_put_char(cmd);
    VCP_put_char(':');
    VCP_put_char(result);
    VCP_put_char(')');
}

void run_rc_car(volatile bool *stop)
{
    led_set(LED_GREEN, true);
    led_set(LED_ORANGE, true);
    led_set(LED_RED, true);
    led_set(LED_BLUE, true);
    engine_stop();

    // Cleanup VCP buffer. This doesn't clean all the garbage.
    // Some garbage arrives much later then we read here.
    uint8_t tmp_char;
    while(VCP_get_char(&tmp_char));

    uint8_t cur_cmd = RC_CAR_CMD_STOP;
    uint8_t new_cmd = cur_cmd;

    while(true) {
        while((!*stop) && !VCP_get_char(&new_cmd)) {}
        if (*stop)
            break;

        if (cur_cmd == new_cmd) {}
        else if (new_cmd == RC_CAR_CMD_STOP)
            engine_stop();
        else if (new_cmd == RC_CAR_CMD_FWD)
            engine_forward();
        else if (new_cmd == RC_CAR_CMD_BKWD)
            engine_backward();
        else if (new_cmd == RC_CAR_CMD_RIGHT)
            engine_turn_right();
        else if (new_cmd == RC_CAR_CMD_LEFT)
            engine_turn_left();
        else {
            send_cmd_result(new_cmd, '1');
            continue;
        }

        send_cmd_result(new_cmd, '0');
        cur_cmd = new_cmd;
    }

    leds_clear_all();
    engine_stop();
}

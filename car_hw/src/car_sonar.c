#include <car_sonar.h>
#include <car_time.h>
#include <usart.h>

/*
 * Sonar reference:
 * http://www.dfrobot.com/wiki/index.php?title=URM37_V4.0_Ultrasonic_Sensor_(SKU:SEN0001)
 */

#define FRONT_ANGLE 1
#define BACK_ANGLE 40
#define MAX_DEGREE 180

static uint8_t sonar_degree(uint8_t degree)
{
    if (degree > MAX_DEGREE)
        degree = MAX_DEGREE;

    degree = MAX_DEGREE - degree;

    float factor = 1.0 * (BACK_ANGLE - FRONT_ANGLE) / MAX_DEGREE;
    return factor * degree + FRONT_ANGLE;
}

#define SONAR_CMD_GET_DIST 0x22

static uint8_t sonar_cmd[4];
static void sonar_snd_dist_req(uint8_t rot_degree)
{
    size_t i = 0;
    sonar_cmd[0] = SONAR_CMD_GET_DIST;
    sonar_cmd[1] = sonar_degree(rot_degree);
    sonar_cmd[2] = sonar_cmd[3] = 0;

    for (; i < 3; ++i)
        sonar_cmd[3] += sonar_cmd[i];
    usart_send_data(USART3_ID, sonar_cmd, 4);
}

static uint8_t sonar_resp[4];
static uint16_t sonar_wait_dist_resp(void)
{
    size_t i = 0;
    uint8_t sum = 0;

    usart_rcv_data(USART3_ID, sonar_resp, 4);
    sum = sonar_resp[0] + sonar_resp[1] + sonar_resp[2];
    if ((sonar_resp[0] != SONAR_CMD_GET_DIST)
            || (sum != sonar_resp[3]))
        return CAR_SONAR_DIST_ERR;

    return ((uint16_t)sonar_resp[1] << 8) + sonar_resp[2];
}

uint16_t car_sonar_get_dist(uint8_t rot_degree)
{
    sonar_snd_dist_req(rot_degree);
    return sonar_wait_dist_resp();
}

void car_sonar_init(void)
{
    usart_init(USART3_ID);
    // Sonar boots in ~2.2 secs
    car_time_wait(3000);
    // Some really needed magical tweak
    sonar_snd_dist_req(0);
}

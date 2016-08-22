#include <car_leds.h>
#include <car_engine.h>
#include <car_time.h>
#include <car_user_btn.h>
#include <car_conn.h>
#include <car_sonar.h>
#include <usart_test.h>

size_t strlen(const char *str)
{
    size_t len = 0;

    while (str[len])
        ++len;
    return len;
}

char digit_to_char(uint8_t digit)
{
    if (digit <= 9)
        return '0' + digit;
    else return 'A' + (digit - 10);
}

// Should allow to store up to "65535\0"
char uint16_str[6] = "65535";

char *uint16_to_str(uint16_t val)
{
    int dix = 4;
    for (; dix >= 0; --dix) {
        uint8_t digit = (uint8_t)(val % 10);
        uint16_str[dix] = digit_to_char(digit);
        val = val / 10;
        if (val == 0)
            break;
    }
    if (dix == -1)
        dix = 0;
    return &uint16_str[dix];
}

void sonar_prog_dist_to_conn(void)
{
    while (1) {
        uint16_t dist = 0;

        car_leds_clear_all();
        dist = car_sonar_get_dist(90);

        if (dist == CAR_SONAR_DIST_ERR)
            car_led_set(CAR_LED_RED, true);
        else {
            car_led_set(CAR_LED_GREEN, true);
            const char *str = uint16_to_str(dist);
            car_conn_snd_buf(strlen(str), (int)str);
            car_conn_snd_byte('\n');
        }
        car_time_wait(1000);
    }
}

void sonar_prog_rotate(void)
{
    uint8_t degree = 0;
    while (1) {
        uint16_t dist = 0;

        car_leds_clear_all();
        dist = car_sonar_get_dist(degree);
        if (degree == 0 ||
            degree == 45 ||
            degree == 90 ||
            degree == (90 + 45) ||
            degree == 180)
            car_time_wait(500);

        degree += 5;
        if (degree > 180)
            degree = 0;
        if (dist == CAR_SONAR_DIST_ERR)
            car_led_set(CAR_LED_RED, true);
        else
            car_led_set(CAR_LED_GREEN, true);
        car_time_wait(300);
    }
}

int main(void)
{
    car_time_init();
    car_leds_init();
    car_engine_init();
    car_user_btn_init(NULL);
    car_conn_init();
    car_sonar_init();

    car_sonar_get_dist(0);
	car_time_wait(3000);
    car_sonar_get_dist(180);
	car_time_wait(3000);
    /* sonar_prog_rotate(); */
    sonar_prog_dist_to_conn();
    // Also we can run usart test from here
}

#include <car_time.h>
#include <car_leds.h>

#include <usart.h>
#include <usart_test.h>
// 2, 1 - ok
// 2, 2 - ok
// 3, 3 - ok
// 2, 3 - ok
// 3, 2 - ok
void usart_test(enum usart_id_t src_usart_id, enum usart_id_t dst_usart_id)
{
    uint8_t byte_sent = 0;
    uint8_t byte_rcvd = 0;
    while (1) {
        car_leds_clear_all();
        car_time_wait(500);

        usart_send_data(src_usart_id, &byte_sent, 1);
        usart_rcv_data(dst_usart_id, &byte_rcvd, 1);
        if (byte_rcvd == byte_sent)
            car_led_set(CAR_LED_GREEN, true);
        else
            car_led_set(CAR_LED_RED, true);
        ++byte_sent;
        car_time_wait(500);
    }
}

#pragma once

#include <stdint.h>

/*
 *       Pins commutation:
 *    MCU   |  URM37 |  Servo  |
 *  ----------------------------
 *    PC11  |  TXD   |    -    |
 *    PC10  |  RXD   |    -    |
 *    GND   |  GND   |    -    |
 *    NRST  |  NRST  |    -    |
 *    5V    |  +5V   |    -    |
 *      -   |  MOTO  |  Orange |
 *    GND   |    -   |  Brown  |
 *    5V    |    -   |  Red    |
 */

#define CAR_SONAR_DIST_ERR 0xffff

void car_sonar_init(void);
// returns CAR_SONAR_DIST_ERR on error
// else distance in centimeters
uint16_t car_sonar_get_dist(uint8_t rot_degree);

uint32_t car_sonar_get_measurement_total();
uint32_t car_sonar_get_measurement_failed_checksum();
uint32_t car_sonar_get_measurement_failed_distance();
uint32_t car_sonar_get_measurement_failed_command();


#include <stddef.h>
#include <stdbool.h>

#include "car_engine.h"
#include "car_leds.h"
#include "car_rc.h"
#include "car_user_btn.h"
#include "time.h"

#include "stm32f4xx_conf.h"
#include "stm32f4xx_it.h"

/* typedef enum { */
/*     CAR_MODE_PROGRAMMED, */
/*     CAR_MODE_REMOTE_CONTROL, */
/*     CAR_MODE_LAST */
/* } CAR_MODE; */
/* static CAR_MODE cur_mode = CAR_MODE_PROGRAMMED; */
/* static __IO bool cur_mode_stop = false; */

/* void stop_cur_mode(void) */
/* { */
/*     cur_mode_stop = true; */
/* } */

//int main(void)
//{
//    /*!< At this stage the microcontroller clock setting is already configured,
//       this is done through SystemInit() function which is called from startup
//       file (startup_stm32f4xx.s) before to branch to application main.
//       To reconfigure the default setting of SystemInit() function, refer to
//       system_stm32f4xx.c file
//     */
//
//    /* time_init(); */
//    /* leds_init(); */
//    /* engine_init(); */
//    /* user_btn_init(stop_cur_mode); */
//    /* VCP_init(); */
//
//    /* while(1) { */
//    /*     if (cur_mode == CAR_MODE_PROGRAMMED) */
//    /*         run_programmed_car(&cur_mode_stop); */
//    /*     else if(cur_mode == CAR_MODE_REMOTE_CONTROL) */
//    /*         run_rc_car(&cur_mode_stop); */
//
//    /*     cur_mode = (cur_mode + 1) % CAR_MODE_LAST; */
//    /*     cur_mode_stop = false; */
//    /* } */
//}

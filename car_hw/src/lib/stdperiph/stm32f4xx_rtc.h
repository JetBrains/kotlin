/**
  ******************************************************************************
  * @file    stm32f4xx_rtc.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the RTC firmware
  *          library.
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ****************************************************************************** 
  */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __STM32F4xx_RTC_H
#define __STM32F4xx_RTC_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup RTC
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/

/** 
  * @brief  RTC Init structures definition  
  */ 
typedef struct
{
  uint32_t RTC_HourFormat;   /*!< Specifies the RTC Hour Format.
                             This parameter can be a value of @ref RTC_Hour_Formats */
  
  uint32_t RTC_AsynchPrediv; /*!< Specifies the RTC Asynchronous Predivider value.
                             This parameter must be set to a value lower than 0x7F */
  
  uint32_t RTC_SynchPrediv;  /*!< Specifies the RTC Synchronous Predivider value.
                             This parameter must be set to a value lower than 0x7FFF */
}RTC_InitTypeDef;

/** 
  * @brief  RTC Time structure definition  
  */
typedef struct
{
  uint8_t RTC_Hours;    /*!< Specifies the RTC Time Hour.
                        This parameter must be set to a value in the 0-12 range
                        if the RTC_HourFormat_12 is selected or 0-23 range if
                        the RTC_HourFormat_24 is selected. */

  uint8_t RTC_Minutes;  /*!< Specifies the RTC Time Minutes.
                        This parameter must be set to a value in the 0-59 range. */
  
  uint8_t RTC_Seconds;  /*!< Specifies the RTC Time Seconds.
                        This parameter must be set to a value in the 0-59 range. */

  uint8_t RTC_H12;      /*!< Specifies the RTC AM/PM Time.
                        This parameter can be a value of @ref RTC_AM_PM_Definitions */
}RTC_TimeTypeDef; 

/** 
  * @brief  RTC Date structure definition  
  */
typedef struct
{
  uint8_t RTC_WeekDay; /*!< Specifies the RTC Date WeekDay.
                        This parameter can be a value of @ref RTC_WeekDay_Definitions */
  
  uint8_t RTC_Month;   /*!< Specifies the RTC Date Month (in BCD format).
                        This parameter can be a value of @ref RTC_Month_Date_Definitions */

  uint8_t RTC_Date;     /*!< Specifies the RTC Date.
                        This parameter must be set to a value in the 1-31 range. */
  
  uint8_t RTC_Year;     /*!< Specifies the RTC Date Year.
                        This parameter must be set to a value in the 0-99 range. */
}RTC_DateTypeDef;

/** 
  * @brief  RTC Alarm structure definition  
  */
typedef struct
{
  RTC_TimeTypeDef RTC_AlarmTime;     /*!< Specifies the RTC Alarm Time members. */

  uint32_t RTC_AlarmMask;            /*!< Specifies the RTC Alarm Masks.
                                     This parameter can be a value of @ref RTC_AlarmMask_Definitions */

  uint32_t RTC_AlarmDateWeekDaySel;  /*!< Specifies the RTC Alarm is on Date or WeekDay.
                                     This parameter can be a value of @ref RTC_AlarmDateWeekDay_Definitions */
  
  uint8_t RTC_AlarmDateWeekDay;      /*!< Specifies the RTC Alarm Date/WeekDay.
                                     If the Alarm Date is selected, this parameter
                                     must be set to a value in the 1-31 range.
                                     If the Alarm WeekDay is selected, this 
                                     parameter can be a value of @ref RTC_WeekDay_Definitions */
}RTC_AlarmTypeDef;

/* Exported constants --------------------------------------------------------*/

/** @defgroup RTC_Exported_Constants
  * @{
  */ 


/** @defgroup RTC_Hour_Formats 
  * @{
  */ 
#define RTC_HourFormat_24              ((uint32_t)0x00000000)
#define RTC_HourFormat_12              ((uint32_t)0x00000040)
#define IS_RTC_HOUR_FORMAT(FORMAT)     (((FORMAT) == RTC_HourFormat_12) || \
                                        ((FORMAT) == RTC_HourFormat_24))
/**
  * @}
  */ 

/** @defgroup RTC_Asynchronous_Predivider 
  * @{
  */ 
#define IS_RTC_ASYNCH_PREDIV(PREDIV)   ((PREDIV) <= 0x7F)
 
/**
  * @}
  */ 


/** @defgroup RTC_Synchronous_Predivider 
  * @{
  */ 
#define IS_RTC_SYNCH_PREDIV(PREDIV)    ((PREDIV) <= 0x7FFF)

/**
  * @}
  */ 

/** @defgroup RTC_Time_Definitions 
  * @{
  */ 
#define IS_RTC_HOUR12(HOUR)            (((HOUR) > 0) && ((HOUR) <= 12))
#define IS_RTC_HOUR24(HOUR)            ((HOUR) <= 23)
#define IS_RTC_MINUTES(MINUTES)        ((MINUTES) <= 59)
#define IS_RTC_SECONDS(SECONDS)        ((SECONDS) <= 59)

/**
  * @}
  */ 

/** @defgroup RTC_AM_PM_Definitions 
  * @{
  */ 
#define RTC_H12_AM                     ((uint8_t)0x00)
#define RTC_H12_PM                     ((uint8_t)0x40)
#define IS_RTC_H12(PM) (((PM) == RTC_H12_AM) || ((PM) == RTC_H12_PM))

/**
  * @}
  */ 

/** @defgroup RTC_Year_Date_Definitions 
  * @{
  */ 
#define IS_RTC_YEAR(YEAR)              ((YEAR) <= 99)

/**
  * @}
  */ 

/** @defgroup RTC_Month_Date_Definitions 
  * @{
  */ 

/* Coded in BCD format */
#define RTC_Month_January              ((uint8_t)0x01)
#define RTC_Month_February             ((uint8_t)0x02)
#define RTC_Month_March                ((uint8_t)0x03)
#define RTC_Month_April                ((uint8_t)0x04)
#define RTC_Month_May                  ((uint8_t)0x05)
#define RTC_Month_June                 ((uint8_t)0x06)
#define RTC_Month_July                 ((uint8_t)0x07)
#define RTC_Month_August               ((uint8_t)0x08)
#define RTC_Month_September            ((uint8_t)0x09)
#define RTC_Month_October              ((uint8_t)0x10)
#define RTC_Month_November             ((uint8_t)0x11)
#define RTC_Month_December             ((uint8_t)0x12)
#define IS_RTC_MONTH(MONTH)            (((MONTH) >= 1) && ((MONTH) <= 12))
#define IS_RTC_DATE(DATE)              (((DATE) >= 1) && ((DATE) <= 31))

/**
  * @}
  */ 

/** @defgroup RTC_WeekDay_Definitions 
  * @{
  */ 
  
#define RTC_Weekday_Monday             ((uint8_t)0x01)
#define RTC_Weekday_Tuesday            ((uint8_t)0x02)
#define RTC_Weekday_Wednesday          ((uint8_t)0x03)
#define RTC_Weekday_Thursday           ((uint8_t)0x04)
#define RTC_Weekday_Friday             ((uint8_t)0x05)
#define RTC_Weekday_Saturday           ((uint8_t)0x06)
#define RTC_Weekday_Sunday             ((uint8_t)0x07)
#define IS_RTC_WEEKDAY(WEEKDAY) (((WEEKDAY) == RTC_Weekday_Monday) || \
                                 ((WEEKDAY) == RTC_Weekday_Tuesday) || \
                                 ((WEEKDAY) == RTC_Weekday_Wednesday) || \
                                 ((WEEKDAY) == RTC_Weekday_Thursday) || \
                                 ((WEEKDAY) == RTC_Weekday_Friday) || \
                                 ((WEEKDAY) == RTC_Weekday_Saturday) || \
                                 ((WEEKDAY) == RTC_Weekday_Sunday))
/**
  * @}
  */ 


/** @defgroup RTC_Alarm_Definitions
  * @{
  */ 
#define IS_RTC_ALARM_DATE_WEEKDAY_DATE(DATE) (((DATE) > 0) && ((DATE) <= 31))
#define IS_RTC_ALARM_DATE_WEEKDAY_WEEKDAY(WEEKDAY) (((WEEKDAY) == RTC_Weekday_Monday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Tuesday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Wednesday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Thursday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Friday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Saturday) || \
                                                    ((WEEKDAY) == RTC_Weekday_Sunday))

/**
  * @}
  */ 


/** @defgroup RTC_AlarmDateWeekDay_Definitions 
  * @{
  */ 
#define RTC_AlarmDateWeekDaySel_Date      ((uint32_t)0x00000000)
#define RTC_AlarmDateWeekDaySel_WeekDay   ((uint32_t)0x40000000)

#define IS_RTC_ALARM_DATE_WEEKDAY_SEL(SEL) (((SEL) == RTC_AlarmDateWeekDaySel_Date) || \
                                            ((SEL) == RTC_AlarmDateWeekDaySel_WeekDay))

/**
  * @}
  */ 


/** @defgroup RTC_AlarmMask_Definitions 
  * @{
  */ 
#define RTC_AlarmMask_None                ((uint32_t)0x00000000)
#define RTC_AlarmMask_DateWeekDay         ((uint32_t)0x80000000)
#define RTC_AlarmMask_Hours               ((uint32_t)0x00800000)
#define RTC_AlarmMask_Minutes             ((uint32_t)0x00008000)
#define RTC_AlarmMask_Seconds             ((uint32_t)0x00000080)
#define RTC_AlarmMask_All                 ((uint32_t)0x80808080)
#define IS_ALARM_MASK(MASK)  (((MASK) & 0x7F7F7F7F) == (uint32_t)RESET)

/**
  * @}
  */ 

/** @defgroup RTC_Alarms_Definitions 
  * @{
  */ 
#define RTC_Alarm_A                       ((uint32_t)0x00000100)
#define RTC_Alarm_B                       ((uint32_t)0x00000200)
#define IS_RTC_ALARM(ALARM)     (((ALARM) == RTC_Alarm_A) || ((ALARM) == RTC_Alarm_B))
#define IS_RTC_CMD_ALARM(ALARM) (((ALARM) & (RTC_Alarm_A | RTC_Alarm_B)) != (uint32_t)RESET)

/**
  * @}
  */ 

  /** @defgroup RTC_Alarm_Sub_Seconds_Masks_Definitions
  * @{
  */ 
#define RTC_AlarmSubSecondMask_All         ((uint32_t)0x00000000) /*!< All Alarm SS fields are masked. 
                                                                       There is no comparison on sub seconds 
                                                                       for Alarm */
#define RTC_AlarmSubSecondMask_SS14_1      ((uint32_t)0x01000000) /*!< SS[14:1] are don't care in Alarm 
                                                                       comparison. Only SS[0] is compared. */
#define RTC_AlarmSubSecondMask_SS14_2      ((uint32_t)0x02000000) /*!< SS[14:2] are don't care in Alarm 
                                                                       comparison. Only SS[1:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_3      ((uint32_t)0x03000000) /*!< SS[14:3] are don't care in Alarm 
                                                                       comparison. Only SS[2:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_4      ((uint32_t)0x04000000) /*!< SS[14:4] are don't care in Alarm 
                                                                       comparison. Only SS[3:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_5      ((uint32_t)0x05000000) /*!< SS[14:5] are don't care in Alarm 
                                                                       comparison. Only SS[4:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_6      ((uint32_t)0x06000000) /*!< SS[14:6] are don't care in Alarm 
                                                                       comparison. Only SS[5:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_7      ((uint32_t)0x07000000) /*!< SS[14:7] are don't care in Alarm 
                                                                       comparison. Only SS[6:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_8      ((uint32_t)0x08000000) /*!< SS[14:8] are don't care in Alarm 
                                                                       comparison. Only SS[7:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_9      ((uint32_t)0x09000000) /*!< SS[14:9] are don't care in Alarm 
                                                                       comparison. Only SS[8:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_10     ((uint32_t)0x0A000000) /*!< SS[14:10] are don't care in Alarm 
                                                                       comparison. Only SS[9:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_11     ((uint32_t)0x0B000000) /*!< SS[14:11] are don't care in Alarm 
                                                                       comparison. Only SS[10:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_12     ((uint32_t)0x0C000000) /*!< SS[14:12] are don't care in Alarm 
                                                                       comparison.Only SS[11:0] are compared */
#define RTC_AlarmSubSecondMask_SS14_13     ((uint32_t)0x0D000000) /*!< SS[14:13] are don't care in Alarm 
                                                                       comparison. Only SS[12:0] are compared */
#define RTC_AlarmSubSecondMask_SS14        ((uint32_t)0x0E000000) /*!< SS[14] is don't care in Alarm 
                                                                       comparison.Only SS[13:0] are compared */
#define RTC_AlarmSubSecondMask_None        ((uint32_t)0x0F000000) /*!< SS[14:0] are compared and must match 
                                                                       to activate alarm. */
#define IS_RTC_ALARM_SUB_SECOND_MASK(MASK)   (((MASK) == RTC_AlarmSubSecondMask_All) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_1) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_2) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_3) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_4) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_5) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_6) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_7) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_8) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_9) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_10) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_11) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_12) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14_13) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_SS14) || \
                                              ((MASK) == RTC_AlarmSubSecondMask_None))
/**
  * @}
  */ 

/** @defgroup RTC_Alarm_Sub_Seconds_Value
  * @{
  */ 

#define IS_RTC_ALARM_SUB_SECOND_VALUE(VALUE) ((VALUE) <= 0x00007FFF)

/**
  * @}
  */ 

/** @defgroup RTC_Wakeup_Timer_Definitions 
  * @{
  */ 
#define RTC_WakeUpClock_RTCCLK_Div16        ((uint32_t)0x00000000)
#define RTC_WakeUpClock_RTCCLK_Div8         ((uint32_t)0x00000001)
#define RTC_WakeUpClock_RTCCLK_Div4         ((uint32_t)0x00000002)
#define RTC_WakeUpClock_RTCCLK_Div2         ((uint32_t)0x00000003)
#define RTC_WakeUpClock_CK_SPRE_16bits      ((uint32_t)0x00000004)
#define RTC_WakeUpClock_CK_SPRE_17bits      ((uint32_t)0x00000006)
#define IS_RTC_WAKEUP_CLOCK(CLOCK) (((CLOCK) == RTC_WakeUpClock_RTCCLK_Div16) || \
                                    ((CLOCK) == RTC_WakeUpClock_RTCCLK_Div8) || \
                                    ((CLOCK) == RTC_WakeUpClock_RTCCLK_Div4) || \
                                    ((CLOCK) == RTC_WakeUpClock_RTCCLK_Div2) || \
                                    ((CLOCK) == RTC_WakeUpClock_CK_SPRE_16bits) || \
                                    ((CLOCK) == RTC_WakeUpClock_CK_SPRE_17bits))
#define IS_RTC_WAKEUP_COUNTER(COUNTER)  ((COUNTER) <= 0xFFFF)
/**
  * @}
  */ 

/** @defgroup RTC_Time_Stamp_Edges_definitions 
  * @{
  */ 
#define RTC_TimeStampEdge_Rising          ((uint32_t)0x00000000)
#define RTC_TimeStampEdge_Falling         ((uint32_t)0x00000008)
#define IS_RTC_TIMESTAMP_EDGE(EDGE) (((EDGE) == RTC_TimeStampEdge_Rising) || \
                                     ((EDGE) == RTC_TimeStampEdge_Falling))
/**
  * @}
  */ 

/** @defgroup RTC_Output_selection_Definitions 
  * @{
  */ 
#define RTC_Output_Disable             ((uint32_t)0x00000000)
#define RTC_Output_AlarmA              ((uint32_t)0x00200000)
#define RTC_Output_AlarmB              ((uint32_t)0x00400000)
#define RTC_Output_WakeUp              ((uint32_t)0x00600000)
 
#define IS_RTC_OUTPUT(OUTPUT) (((OUTPUT) == RTC_Output_Disable) || \
                               ((OUTPUT) == RTC_Output_AlarmA) || \
                               ((OUTPUT) == RTC_Output_AlarmB) || \
                               ((OUTPUT) == RTC_Output_WakeUp))

/**
  * @}
  */ 

/** @defgroup RTC_Output_Polarity_Definitions 
  * @{
  */ 
#define RTC_OutputPolarity_High           ((uint32_t)0x00000000)
#define RTC_OutputPolarity_Low            ((uint32_t)0x00100000)
#define IS_RTC_OUTPUT_POL(POL) (((POL) == RTC_OutputPolarity_High) || \
                                ((POL) == RTC_OutputPolarity_Low))
/**
  * @}
  */ 


/** @defgroup RTC_Digital_Calibration_Definitions 
  * @{
  */ 
#define RTC_CalibSign_Positive            ((uint32_t)0x00000000) 
#define RTC_CalibSign_Negative            ((uint32_t)0x00000080)
#define IS_RTC_CALIB_SIGN(SIGN) (((SIGN) == RTC_CalibSign_Positive) || \
                                 ((SIGN) == RTC_CalibSign_Negative))
#define IS_RTC_CALIB_VALUE(VALUE) ((VALUE) < 0x20)

/**
  * @}
  */ 

 /** @defgroup RTC_Calib_Output_selection_Definitions 
  * @{
  */ 
#define RTC_CalibOutput_512Hz            ((uint32_t)0x00000000) 
#define RTC_CalibOutput_1Hz              ((uint32_t)0x00080000)
#define IS_RTC_CALIB_OUTPUT(OUTPUT)  (((OUTPUT) == RTC_CalibOutput_512Hz) || \
                                      ((OUTPUT) == RTC_CalibOutput_1Hz))
/**
  * @}
  */ 

/** @defgroup RTC_Smooth_calib_period_Definitions 
  * @{
  */ 
#define RTC_SmoothCalibPeriod_32sec   ((uint32_t)0x00000000) /*!<  if RTCCLK = 32768 Hz, Smooth calibation
                                                             period is 32s,  else 2exp20 RTCCLK seconds */
#define RTC_SmoothCalibPeriod_16sec   ((uint32_t)0x00002000) /*!<  if RTCCLK = 32768 Hz, Smooth calibation 
                                                             period is 16s, else 2exp19 RTCCLK seconds */
#define RTC_SmoothCalibPeriod_8sec    ((uint32_t)0x00004000) /*!<  if RTCCLK = 32768 Hz, Smooth calibation 
                                                             period is 8s, else 2exp18 RTCCLK seconds */
#define IS_RTC_SMOOTH_CALIB_PERIOD(PERIOD) (((PERIOD) == RTC_SmoothCalibPeriod_32sec) || \
                                             ((PERIOD) == RTC_SmoothCalibPeriod_16sec) || \
                                             ((PERIOD) == RTC_SmoothCalibPeriod_8sec))
                                          
/**
  * @}
  */ 

/** @defgroup RTC_Smooth_calib_Plus_pulses_Definitions 
  * @{
  */ 
#define RTC_SmoothCalibPlusPulses_Set    ((uint32_t)0x00008000) /*!<  The number of RTCCLK pulses added  
                                                                during a X -second window = Y - CALM[8:0]. 
                                                                 with Y = 512, 256, 128 when X = 32, 16, 8 */
#define RTC_SmoothCalibPlusPulses_Reset  ((uint32_t)0x00000000) /*!<  The number of RTCCLK pulses subbstited
                                                                 during a 32-second window =   CALM[8:0]. */
#define IS_RTC_SMOOTH_CALIB_PLUS(PLUS) (((PLUS) == RTC_SmoothCalibPlusPulses_Set) || \
                                         ((PLUS) == RTC_SmoothCalibPlusPulses_Reset))

/**
  * @}
  */ 

/** @defgroup RTC_Smooth_calib_Minus_pulses_Definitions 
  * @{
  */ 
#define  IS_RTC_SMOOTH_CALIB_MINUS(VALUE) ((VALUE) <= 0x000001FF)

/**
  * @}
  */

/** @defgroup RTC_DayLightSaving_Definitions 
  * @{
  */ 
#define RTC_DayLightSaving_SUB1H   ((uint32_t)0x00020000)
#define RTC_DayLightSaving_ADD1H   ((uint32_t)0x00010000)
#define IS_RTC_DAYLIGHT_SAVING(SAVE) (((SAVE) == RTC_DayLightSaving_SUB1H) || \
                                      ((SAVE) == RTC_DayLightSaving_ADD1H))

#define RTC_StoreOperation_Reset        ((uint32_t)0x00000000)
#define RTC_StoreOperation_Set          ((uint32_t)0x00040000)
#define IS_RTC_STORE_OPERATION(OPERATION) (((OPERATION) == RTC_StoreOperation_Reset) || \
                                           ((OPERATION) == RTC_StoreOperation_Set))
/**
  * @}
  */ 

/** @defgroup RTC_Tamper_Trigger_Definitions 
  * @{
  */ 
#define RTC_TamperTrigger_RisingEdge            ((uint32_t)0x00000000)
#define RTC_TamperTrigger_FallingEdge           ((uint32_t)0x00000001)
#define RTC_TamperTrigger_LowLevel              ((uint32_t)0x00000000)
#define RTC_TamperTrigger_HighLevel             ((uint32_t)0x00000001)
#define IS_RTC_TAMPER_TRIGGER(TRIGGER) (((TRIGGER) == RTC_TamperTrigger_RisingEdge) || \
                                        ((TRIGGER) == RTC_TamperTrigger_FallingEdge) || \
                                        ((TRIGGER) == RTC_TamperTrigger_LowLevel) || \
                                        ((TRIGGER) == RTC_TamperTrigger_HighLevel)) 

/**
  * @}
  */ 

/** @defgroup RTC_Tamper_Filter_Definitions 
  * @{
  */ 
#define RTC_TamperFilter_Disable   ((uint32_t)0x00000000) /*!< Tamper filter is disabled */

#define RTC_TamperFilter_2Sample   ((uint32_t)0x00000800) /*!< Tamper is activated after 2 
                                                          consecutive samples at the active level */
#define RTC_TamperFilter_4Sample   ((uint32_t)0x00001000) /*!< Tamper is activated after 4 
                                                          consecutive samples at the active level */
#define RTC_TamperFilter_8Sample   ((uint32_t)0x00001800) /*!< Tamper is activated after 8 
                                                          consecutive samples at the active leve. */
#define IS_RTC_TAMPER_FILTER(FILTER) (((FILTER) == RTC_TamperFilter_Disable) || \
                                      ((FILTER) == RTC_TamperFilter_2Sample) || \
                                      ((FILTER) == RTC_TamperFilter_4Sample) || \
                                      ((FILTER) == RTC_TamperFilter_8Sample))
/**
  * @}
  */ 

/** @defgroup RTC_Tamper_Sampling_Frequencies_Definitions 
  * @{
  */ 
#define RTC_TamperSamplingFreq_RTCCLK_Div32768  ((uint32_t)0x00000000) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 32768 */
#define RTC_TamperSamplingFreq_RTCCLK_Div16384  ((uint32_t)0x000000100) /*!< Each of the tamper inputs are sampled
                                                                            with a frequency =  RTCCLK / 16384 */
#define RTC_TamperSamplingFreq_RTCCLK_Div8192   ((uint32_t)0x00000200) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 8192  */
#define RTC_TamperSamplingFreq_RTCCLK_Div4096   ((uint32_t)0x00000300) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 4096  */
#define RTC_TamperSamplingFreq_RTCCLK_Div2048   ((uint32_t)0x00000400) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 2048  */
#define RTC_TamperSamplingFreq_RTCCLK_Div1024   ((uint32_t)0x00000500) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 1024  */
#define RTC_TamperSamplingFreq_RTCCLK_Div512    ((uint32_t)0x00000600) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 512   */
#define RTC_TamperSamplingFreq_RTCCLK_Div256    ((uint32_t)0x00000700) /*!< Each of the tamper inputs are sampled
                                                                           with a frequency =  RTCCLK / 256   */
#define IS_RTC_TAMPER_SAMPLING_FREQ(FREQ) (((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div32768) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div16384) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div8192) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div4096) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div2048) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div1024) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div512) || \
                                           ((FREQ) ==RTC_TamperSamplingFreq_RTCCLK_Div256))

/**
  * @}
  */

  /** @defgroup RTC_Tamper_Pin_Precharge_Duration_Definitions 
  * @{
  */ 
#define RTC_TamperPrechargeDuration_1RTCCLK ((uint32_t)0x00000000)  /*!< Tamper pins are pre-charged before 
                                                                         sampling during 1 RTCCLK cycle */
#define RTC_TamperPrechargeDuration_2RTCCLK ((uint32_t)0x00002000)  /*!< Tamper pins are pre-charged before 
                                                                         sampling during 2 RTCCLK cycles */
#define RTC_TamperPrechargeDuration_4RTCCLK ((uint32_t)0x00004000)  /*!< Tamper pins are pre-charged before 
                                                                         sampling during 4 RTCCLK cycles */
#define RTC_TamperPrechargeDuration_8RTCCLK ((uint32_t)0x00006000)  /*!< Tamper pins are pre-charged before 
                                                                         sampling during 8 RTCCLK cycles */

#define IS_RTC_TAMPER_PRECHARGE_DURATION(DURATION) (((DURATION) == RTC_TamperPrechargeDuration_1RTCCLK) || \
                                                    ((DURATION) == RTC_TamperPrechargeDuration_2RTCCLK) || \
                                                    ((DURATION) == RTC_TamperPrechargeDuration_4RTCCLK) || \
                                                    ((DURATION) == RTC_TamperPrechargeDuration_8RTCCLK))
/**
  * @}
  */

/** @defgroup RTC_Tamper_Pins_Definitions 
  * @{
  */ 
#define RTC_Tamper_1                    RTC_TAFCR_TAMP1E
#define IS_RTC_TAMPER(TAMPER) (((TAMPER) == RTC_Tamper_1))

/**
  * @}
  */

/** @defgroup RTC_Tamper_Pin_Selection 
  * @{
  */ 
#define RTC_TamperPin_PC13                 ((uint32_t)0x00000000)
#define RTC_TamperPin_PI8                  ((uint32_t)0x00010000)
#define IS_RTC_TAMPER_PIN(PIN) (((PIN) == RTC_TamperPin_PC13) || \
                                ((PIN) == RTC_TamperPin_PI8))
/**
  * @}
  */ 

/** @defgroup RTC_TimeStamp_Pin_Selection 
  * @{
  */ 
#define RTC_TimeStampPin_PC13              ((uint32_t)0x00000000)
#define RTC_TimeStampPin_PI8               ((uint32_t)0x00020000)
#define IS_RTC_TIMESTAMP_PIN(PIN) (((PIN) == RTC_TimeStampPin_PC13) || \
                                   ((PIN) == RTC_TimeStampPin_PI8))
/**
  * @}
  */ 

/** @defgroup RTC_Output_Type_ALARM_OUT 
  * @{
  */ 
#define RTC_OutputType_OpenDrain           ((uint32_t)0x00000000)
#define RTC_OutputType_PushPull            ((uint32_t)0x00040000)
#define IS_RTC_OUTPUT_TYPE(TYPE) (((TYPE) == RTC_OutputType_OpenDrain) || \
                                  ((TYPE) == RTC_OutputType_PushPull))

/**
  * @}
  */ 

/** @defgroup RTC_Add_1_Second_Parameter_Definitions
  * @{
  */ 
#define RTC_ShiftAdd1S_Reset      ((uint32_t)0x00000000)
#define RTC_ShiftAdd1S_Set        ((uint32_t)0x80000000)
#define IS_RTC_SHIFT_ADD1S(SEL) (((SEL) == RTC_ShiftAdd1S_Reset) || \
                                 ((SEL) == RTC_ShiftAdd1S_Set))
/**
  * @}
  */ 

/** @defgroup RTC_Substract_Fraction_Of_Second_Value
  * @{
  */ 
#define IS_RTC_SHIFT_SUBFS(FS) ((FS) <= 0x00007FFF)

/**
  * @}
  */

/** @defgroup RTC_Backup_Registers_Definitions 
  * @{
  */

#define RTC_BKP_DR0                       ((uint32_t)0x00000000)
#define RTC_BKP_DR1                       ((uint32_t)0x00000001)
#define RTC_BKP_DR2                       ((uint32_t)0x00000002)
#define RTC_BKP_DR3                       ((uint32_t)0x00000003)
#define RTC_BKP_DR4                       ((uint32_t)0x00000004)
#define RTC_BKP_DR5                       ((uint32_t)0x00000005)
#define RTC_BKP_DR6                       ((uint32_t)0x00000006)
#define RTC_BKP_DR7                       ((uint32_t)0x00000007)
#define RTC_BKP_DR8                       ((uint32_t)0x00000008)
#define RTC_BKP_DR9                       ((uint32_t)0x00000009)
#define RTC_BKP_DR10                      ((uint32_t)0x0000000A)
#define RTC_BKP_DR11                      ((uint32_t)0x0000000B)
#define RTC_BKP_DR12                      ((uint32_t)0x0000000C)
#define RTC_BKP_DR13                      ((uint32_t)0x0000000D)
#define RTC_BKP_DR14                      ((uint32_t)0x0000000E)
#define RTC_BKP_DR15                      ((uint32_t)0x0000000F)
#define RTC_BKP_DR16                      ((uint32_t)0x00000010)
#define RTC_BKP_DR17                      ((uint32_t)0x00000011)
#define RTC_BKP_DR18                      ((uint32_t)0x00000012)
#define RTC_BKP_DR19                      ((uint32_t)0x00000013)
#define IS_RTC_BKP(BKP)                   (((BKP) == RTC_BKP_DR0) || \
                                           ((BKP) == RTC_BKP_DR1) || \
                                           ((BKP) == RTC_BKP_DR2) || \
                                           ((BKP) == RTC_BKP_DR3) || \
                                           ((BKP) == RTC_BKP_DR4) || \
                                           ((BKP) == RTC_BKP_DR5) || \
                                           ((BKP) == RTC_BKP_DR6) || \
                                           ((BKP) == RTC_BKP_DR7) || \
                                           ((BKP) == RTC_BKP_DR8) || \
                                           ((BKP) == RTC_BKP_DR9) || \
                                           ((BKP) == RTC_BKP_DR10) || \
                                           ((BKP) == RTC_BKP_DR11) || \
                                           ((BKP) == RTC_BKP_DR12) || \
                                           ((BKP) == RTC_BKP_DR13) || \
                                           ((BKP) == RTC_BKP_DR14) || \
                                           ((BKP) == RTC_BKP_DR15) || \
                                           ((BKP) == RTC_BKP_DR16) || \
                                           ((BKP) == RTC_BKP_DR17) || \
                                           ((BKP) == RTC_BKP_DR18) || \
                                           ((BKP) == RTC_BKP_DR19))
/**
  * @}
  */ 

/** @defgroup RTC_Input_parameter_format_definitions 
  * @{
  */ 
#define RTC_Format_BIN                    ((uint32_t)0x000000000)
#define RTC_Format_BCD                    ((uint32_t)0x000000001)
#define IS_RTC_FORMAT(FORMAT) (((FORMAT) == RTC_Format_BIN) || ((FORMAT) == RTC_Format_BCD))

/**
  * @}
  */ 

/** @defgroup RTC_Flags_Definitions 
  * @{
  */ 
#define RTC_FLAG_RECALPF                  ((uint32_t)0x00010000)
#define RTC_FLAG_TAMP1F                   ((uint32_t)0x00002000)
#define RTC_FLAG_TSOVF                    ((uint32_t)0x00001000)
#define RTC_FLAG_TSF                      ((uint32_t)0x00000800)
#define RTC_FLAG_WUTF                     ((uint32_t)0x00000400)
#define RTC_FLAG_ALRBF                    ((uint32_t)0x00000200)
#define RTC_FLAG_ALRAF                    ((uint32_t)0x00000100)
#define RTC_FLAG_INITF                    ((uint32_t)0x00000040)
#define RTC_FLAG_RSF                      ((uint32_t)0x00000020)
#define RTC_FLAG_INITS                    ((uint32_t)0x00000010)
#define RTC_FLAG_SHPF                     ((uint32_t)0x00000008)
#define RTC_FLAG_WUTWF                    ((uint32_t)0x00000004)
#define RTC_FLAG_ALRBWF                   ((uint32_t)0x00000002)
#define RTC_FLAG_ALRAWF                   ((uint32_t)0x00000001)
#define IS_RTC_GET_FLAG(FLAG) (((FLAG) == RTC_FLAG_TSOVF) || ((FLAG) == RTC_FLAG_TSF) || \
                               ((FLAG) == RTC_FLAG_WUTF) || ((FLAG) == RTC_FLAG_ALRBF) || \
                               ((FLAG) == RTC_FLAG_ALRAF) || ((FLAG) == RTC_FLAG_INITF) || \
                               ((FLAG) == RTC_FLAG_RSF) || ((FLAG) == RTC_FLAG_WUTWF) || \
                               ((FLAG) == RTC_FLAG_ALRBWF) || ((FLAG) == RTC_FLAG_ALRAWF) || \
                               ((FLAG) == RTC_FLAG_TAMP1F) || ((FLAG) == RTC_FLAG_RECALPF) || \
                                ((FLAG) == RTC_FLAG_SHPF))
#define IS_RTC_CLEAR_FLAG(FLAG) (((FLAG) != (uint32_t)RESET) && (((FLAG) & 0xFFFF00DF) == (uint32_t)RESET))
/**
  * @}
  */ 

/** @defgroup RTC_Interrupts_Definitions 
  * @{
  */ 
#define RTC_IT_TS                         ((uint32_t)0x00008000)
#define RTC_IT_WUT                        ((uint32_t)0x00004000)
#define RTC_IT_ALRB                       ((uint32_t)0x00002000)
#define RTC_IT_ALRA                       ((uint32_t)0x00001000)
#define RTC_IT_TAMP                       ((uint32_t)0x00000004) /* Used only to Enable the Tamper Interrupt */
#define RTC_IT_TAMP1                      ((uint32_t)0x00020000)

#define IS_RTC_CONFIG_IT(IT) (((IT) != (uint32_t)RESET) && (((IT) & 0xFFFF0FFB) == (uint32_t)RESET))
#define IS_RTC_GET_IT(IT) (((IT) == RTC_IT_TS) || ((IT) == RTC_IT_WUT) || \
                           ((IT) == RTC_IT_ALRB) || ((IT) == RTC_IT_ALRA) || \
                           ((IT) == RTC_IT_TAMP1))
#define IS_RTC_CLEAR_IT(IT) (((IT) != (uint32_t)RESET) && (((IT) & 0xFFFD0FFF) == (uint32_t)RESET))

/**
  * @}
  */ 

/** @defgroup RTC_Legacy 
  * @{
  */ 
#define RTC_DigitalCalibConfig  RTC_CoarseCalibConfig
#define RTC_DigitalCalibCmd     RTC_CoarseCalibCmd

/**
  * @}
  */ 

/**
  * @}
  */ 

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/ 

/*  Function used to set the RTC configuration to the default reset state *****/
ErrorStatus RTC_DeInit(void);

/* Initialization and Configuration functions *********************************/
ErrorStatus RTC_Init(RTC_InitTypeDef* RTC_InitStruct);
void RTC_StructInit(RTC_InitTypeDef* RTC_InitStruct);
void RTC_WriteProtectionCmd(FunctionalState NewState);
ErrorStatus RTC_EnterInitMode(void);
void RTC_ExitInitMode(void);
ErrorStatus RTC_WaitForSynchro(void);
ErrorStatus RTC_RefClockCmd(FunctionalState NewState);
void RTC_BypassShadowCmd(FunctionalState NewState);

/* Time and Date configuration functions **************************************/
ErrorStatus RTC_SetTime(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_TimeStruct);
void RTC_TimeStructInit(RTC_TimeTypeDef* RTC_TimeStruct);
void RTC_GetTime(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_TimeStruct);
uint32_t RTC_GetSubSecond(void);
ErrorStatus RTC_SetDate(uint32_t RTC_Format, RTC_DateTypeDef* RTC_DateStruct);
void RTC_DateStructInit(RTC_DateTypeDef* RTC_DateStruct);
void RTC_GetDate(uint32_t RTC_Format, RTC_DateTypeDef* RTC_DateStruct);

/* Alarms (Alarm A and Alarm B) configuration functions  **********************/
void RTC_SetAlarm(uint32_t RTC_Format, uint32_t RTC_Alarm, RTC_AlarmTypeDef* RTC_AlarmStruct);
void RTC_AlarmStructInit(RTC_AlarmTypeDef* RTC_AlarmStruct);
void RTC_GetAlarm(uint32_t RTC_Format, uint32_t RTC_Alarm, RTC_AlarmTypeDef* RTC_AlarmStruct);
ErrorStatus RTC_AlarmCmd(uint32_t RTC_Alarm, FunctionalState NewState);
void RTC_AlarmSubSecondConfig(uint32_t RTC_Alarm, uint32_t RTC_AlarmSubSecondValue, uint32_t RTC_AlarmSubSecondMask);
uint32_t RTC_GetAlarmSubSecond(uint32_t RTC_Alarm);

/* WakeUp Timer configuration functions ***************************************/
void RTC_WakeUpClockConfig(uint32_t RTC_WakeUpClock);
void RTC_SetWakeUpCounter(uint32_t RTC_WakeUpCounter);
uint32_t RTC_GetWakeUpCounter(void);
ErrorStatus RTC_WakeUpCmd(FunctionalState NewState);

/* Daylight Saving configuration functions ************************************/
void RTC_DayLightSavingConfig(uint32_t RTC_DayLightSaving, uint32_t RTC_StoreOperation);
uint32_t RTC_GetStoreOperation(void);

/* Output pin Configuration function ******************************************/
void RTC_OutputConfig(uint32_t RTC_Output, uint32_t RTC_OutputPolarity);

/* Digital Calibration configuration functions *********************************/
ErrorStatus RTC_CoarseCalibConfig(uint32_t RTC_CalibSign, uint32_t Value);
ErrorStatus RTC_CoarseCalibCmd(FunctionalState NewState);
void RTC_CalibOutputCmd(FunctionalState NewState);
void RTC_CalibOutputConfig(uint32_t RTC_CalibOutput);
ErrorStatus RTC_SmoothCalibConfig(uint32_t RTC_SmoothCalibPeriod, 
                                  uint32_t RTC_SmoothCalibPlusPulses,
                                  uint32_t RTC_SmouthCalibMinusPulsesValue);

/* TimeStamp configuration functions ******************************************/
void RTC_TimeStampCmd(uint32_t RTC_TimeStampEdge, FunctionalState NewState);
void RTC_GetTimeStamp(uint32_t RTC_Format, RTC_TimeTypeDef* RTC_StampTimeStruct,
                                      RTC_DateTypeDef* RTC_StampDateStruct);
uint32_t RTC_GetTimeStampSubSecond(void);

/* Tampers configuration functions ********************************************/
void RTC_TamperTriggerConfig(uint32_t RTC_Tamper, uint32_t RTC_TamperTrigger);
void RTC_TamperCmd(uint32_t RTC_Tamper, FunctionalState NewState);
void RTC_TamperFilterConfig(uint32_t RTC_TamperFilter);
void RTC_TamperSamplingFreqConfig(uint32_t RTC_TamperSamplingFreq);
void RTC_TamperPinsPrechargeDuration(uint32_t RTC_TamperPrechargeDuration);
void RTC_TimeStampOnTamperDetectionCmd(FunctionalState NewState);
void RTC_TamperPullUpCmd(FunctionalState NewState);

/* Backup Data Registers configuration functions ******************************/
void RTC_WriteBackupRegister(uint32_t RTC_BKP_DR, uint32_t Data);
uint32_t RTC_ReadBackupRegister(uint32_t RTC_BKP_DR);

/* RTC Tamper and TimeStamp Pins Selection and Output Type Config configuration
   functions ******************************************************************/
void RTC_TamperPinSelection(uint32_t RTC_TamperPin);
void RTC_TimeStampPinSelection(uint32_t RTC_TimeStampPin);
void RTC_OutputTypeConfig(uint32_t RTC_OutputType);

/* RTC_Shift_control_synchonisation_functions *********************************/
ErrorStatus RTC_SynchroShiftConfig(uint32_t RTC_ShiftAdd1S, uint32_t RTC_ShiftSubFS);

/* Interrupts and flags management functions **********************************/
void RTC_ITConfig(uint32_t RTC_IT, FunctionalState NewState);
FlagStatus RTC_GetFlagStatus(uint32_t RTC_FLAG);
void RTC_ClearFlag(uint32_t RTC_FLAG);
ITStatus RTC_GetITStatus(uint32_t RTC_IT);
void RTC_ClearITPendingBit(uint32_t RTC_IT);

#ifdef __cplusplus
}
#endif

#endif /*__STM32F4xx_RTC_H */

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

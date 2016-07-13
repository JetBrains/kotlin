/**
  ******************************************************************************
  * @file    stm32f4xx_adc.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the ADC firmware 
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
#ifndef __STM32F4xx_ADC_H
#define __STM32F4xx_ADC_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup ADC
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/

/** 
  * @brief   ADC Init structure definition  
  */ 
typedef struct
{
  uint32_t ADC_Resolution;                /*!< Configures the ADC resolution dual mode. 
                                               This parameter can be a value of @ref ADC_resolution */                                   
  FunctionalState ADC_ScanConvMode;       /*!< Specifies whether the conversion 
                                               is performed in Scan (multichannels) 
                                               or Single (one channel) mode.
                                               This parameter can be set to ENABLE or DISABLE */ 
  FunctionalState ADC_ContinuousConvMode; /*!< Specifies whether the conversion 
                                               is performed in Continuous or Single mode.
                                               This parameter can be set to ENABLE or DISABLE. */
  uint32_t ADC_ExternalTrigConvEdge;      /*!< Select the external trigger edge and
                                               enable the trigger of a regular group. 
                                               This parameter can be a value of 
                                               @ref ADC_external_trigger_edge_for_regular_channels_conversion */
  uint32_t ADC_ExternalTrigConv;          /*!< Select the external event used to trigger 
                                               the start of conversion of a regular group.
                                               This parameter can be a value of 
                                               @ref ADC_extrenal_trigger_sources_for_regular_channels_conversion */
  uint32_t ADC_DataAlign;                 /*!< Specifies whether the ADC data  alignment
                                               is left or right. This parameter can be 
                                               a value of @ref ADC_data_align */
  uint8_t  ADC_NbrOfConversion;           /*!< Specifies the number of ADC conversions
                                               that will be done using the sequencer for
                                               regular channel group.
                                               This parameter must range from 1 to 16. */
}ADC_InitTypeDef;
  
/** 
  * @brief   ADC Common Init structure definition  
  */ 
typedef struct 
{
  uint32_t ADC_Mode;                      /*!< Configures the ADC to operate in 
                                               independent or multi mode. 
                                               This parameter can be a value of @ref ADC_Common_mode */                                              
  uint32_t ADC_Prescaler;                 /*!< Select the frequency of the clock 
                                               to the ADC. The clock is common for all the ADCs.
                                               This parameter can be a value of @ref ADC_Prescaler */
  uint32_t ADC_DMAAccessMode;             /*!< Configures the Direct memory access 
                                              mode for multi ADC mode.
                                               This parameter can be a value of 
                                               @ref ADC_Direct_memory_access_mode_for_multi_mode */
  uint32_t ADC_TwoSamplingDelay;          /*!< Configures the Delay between 2 sampling phases.
                                               This parameter can be a value of 
                                               @ref ADC_delay_between_2_sampling_phases */
  
}ADC_CommonInitTypeDef;


/* Exported constants --------------------------------------------------------*/

/** @defgroup ADC_Exported_Constants
  * @{
  */ 
#define IS_ADC_ALL_PERIPH(PERIPH) (((PERIPH) == ADC1) || \
                                   ((PERIPH) == ADC2) || \
                                   ((PERIPH) == ADC3))  

/** @defgroup ADC_Common_mode 
  * @{
  */ 
#define ADC_Mode_Independent                       ((uint32_t)0x00000000)       
#define ADC_DualMode_RegSimult_InjecSimult         ((uint32_t)0x00000001)
#define ADC_DualMode_RegSimult_AlterTrig           ((uint32_t)0x00000002)
#define ADC_DualMode_InjecSimult                   ((uint32_t)0x00000005)
#define ADC_DualMode_RegSimult                     ((uint32_t)0x00000006)
#define ADC_DualMode_Interl                        ((uint32_t)0x00000007)
#define ADC_DualMode_AlterTrig                     ((uint32_t)0x00000009)
#define ADC_TripleMode_RegSimult_InjecSimult       ((uint32_t)0x00000011)
#define ADC_TripleMode_RegSimult_AlterTrig         ((uint32_t)0x00000012)
#define ADC_TripleMode_InjecSimult                 ((uint32_t)0x00000015)
#define ADC_TripleMode_RegSimult                   ((uint32_t)0x00000016)
#define ADC_TripleMode_Interl                      ((uint32_t)0x00000017)
#define ADC_TripleMode_AlterTrig                   ((uint32_t)0x00000019)
#define IS_ADC_MODE(MODE) (((MODE) == ADC_Mode_Independent) || \
                           ((MODE) == ADC_DualMode_RegSimult_InjecSimult) || \
                           ((MODE) == ADC_DualMode_RegSimult_AlterTrig) || \
                           ((MODE) == ADC_DualMode_InjecSimult) || \
                           ((MODE) == ADC_DualMode_RegSimult) || \
                           ((MODE) == ADC_DualMode_Interl) || \
                           ((MODE) == ADC_DualMode_AlterTrig) || \
                           ((MODE) == ADC_TripleMode_RegSimult_InjecSimult) || \
                           ((MODE) == ADC_TripleMode_RegSimult_AlterTrig) || \
                           ((MODE) == ADC_TripleMode_InjecSimult) || \
                           ((MODE) == ADC_TripleMode_RegSimult) || \
                           ((MODE) == ADC_TripleMode_Interl) || \
                           ((MODE) == ADC_TripleMode_AlterTrig))
/**
  * @}
  */ 


/** @defgroup ADC_Prescaler 
  * @{
  */ 
#define ADC_Prescaler_Div2                         ((uint32_t)0x00000000)
#define ADC_Prescaler_Div4                         ((uint32_t)0x00010000)
#define ADC_Prescaler_Div6                         ((uint32_t)0x00020000)
#define ADC_Prescaler_Div8                         ((uint32_t)0x00030000)
#define IS_ADC_PRESCALER(PRESCALER) (((PRESCALER) == ADC_Prescaler_Div2) || \
                                     ((PRESCALER) == ADC_Prescaler_Div4) || \
                                     ((PRESCALER) == ADC_Prescaler_Div6) || \
                                     ((PRESCALER) == ADC_Prescaler_Div8))
/**
  * @}
  */ 


/** @defgroup ADC_Direct_memory_access_mode_for_multi_mode 
  * @{
  */ 
#define ADC_DMAAccessMode_Disabled      ((uint32_t)0x00000000)     /* DMA mode disabled */
#define ADC_DMAAccessMode_1             ((uint32_t)0x00004000)     /* DMA mode 1 enabled (2 / 3 half-words one by one - 1 then 2 then 3)*/
#define ADC_DMAAccessMode_2             ((uint32_t)0x00008000)     /* DMA mode 2 enabled (2 / 3 half-words by pairs - 2&1 then 1&3 then 3&2)*/
#define ADC_DMAAccessMode_3             ((uint32_t)0x0000C000)     /* DMA mode 3 enabled (2 / 3 bytes by pairs - 2&1 then 1&3 then 3&2) */
#define IS_ADC_DMA_ACCESS_MODE(MODE) (((MODE) == ADC_DMAAccessMode_Disabled) || \
                                      ((MODE) == ADC_DMAAccessMode_1) || \
                                      ((MODE) == ADC_DMAAccessMode_2) || \
                                      ((MODE) == ADC_DMAAccessMode_3))
                                     
/**
  * @}
  */ 


/** @defgroup ADC_delay_between_2_sampling_phases 
  * @{
  */ 
#define ADC_TwoSamplingDelay_5Cycles               ((uint32_t)0x00000000)
#define ADC_TwoSamplingDelay_6Cycles               ((uint32_t)0x00000100)
#define ADC_TwoSamplingDelay_7Cycles               ((uint32_t)0x00000200)
#define ADC_TwoSamplingDelay_8Cycles               ((uint32_t)0x00000300)
#define ADC_TwoSamplingDelay_9Cycles               ((uint32_t)0x00000400)
#define ADC_TwoSamplingDelay_10Cycles              ((uint32_t)0x00000500)
#define ADC_TwoSamplingDelay_11Cycles              ((uint32_t)0x00000600)
#define ADC_TwoSamplingDelay_12Cycles              ((uint32_t)0x00000700)
#define ADC_TwoSamplingDelay_13Cycles              ((uint32_t)0x00000800)
#define ADC_TwoSamplingDelay_14Cycles              ((uint32_t)0x00000900)
#define ADC_TwoSamplingDelay_15Cycles              ((uint32_t)0x00000A00)
#define ADC_TwoSamplingDelay_16Cycles              ((uint32_t)0x00000B00)
#define ADC_TwoSamplingDelay_17Cycles              ((uint32_t)0x00000C00)
#define ADC_TwoSamplingDelay_18Cycles              ((uint32_t)0x00000D00)
#define ADC_TwoSamplingDelay_19Cycles              ((uint32_t)0x00000E00)
#define ADC_TwoSamplingDelay_20Cycles              ((uint32_t)0x00000F00)
#define IS_ADC_SAMPLING_DELAY(DELAY) (((DELAY) == ADC_TwoSamplingDelay_5Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_6Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_7Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_8Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_9Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_10Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_11Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_12Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_13Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_14Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_15Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_16Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_17Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_18Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_19Cycles) || \
                                      ((DELAY) == ADC_TwoSamplingDelay_20Cycles))
                                     
/**
  * @}
  */ 


/** @defgroup ADC_resolution 
  * @{
  */ 
#define ADC_Resolution_12b                         ((uint32_t)0x00000000)
#define ADC_Resolution_10b                         ((uint32_t)0x01000000)
#define ADC_Resolution_8b                          ((uint32_t)0x02000000)
#define ADC_Resolution_6b                          ((uint32_t)0x03000000)
#define IS_ADC_RESOLUTION(RESOLUTION) (((RESOLUTION) == ADC_Resolution_12b) || \
                                       ((RESOLUTION) == ADC_Resolution_10b) || \
                                       ((RESOLUTION) == ADC_Resolution_8b) || \
                                       ((RESOLUTION) == ADC_Resolution_6b))
                                      
/**
  * @}
  */ 


/** @defgroup ADC_external_trigger_edge_for_regular_channels_conversion 
  * @{
  */ 
#define ADC_ExternalTrigConvEdge_None          ((uint32_t)0x00000000)
#define ADC_ExternalTrigConvEdge_Rising        ((uint32_t)0x10000000)
#define ADC_ExternalTrigConvEdge_Falling       ((uint32_t)0x20000000)
#define ADC_ExternalTrigConvEdge_RisingFalling ((uint32_t)0x30000000)
#define IS_ADC_EXT_TRIG_EDGE(EDGE) (((EDGE) == ADC_ExternalTrigConvEdge_None) || \
                             ((EDGE) == ADC_ExternalTrigConvEdge_Rising) || \
                             ((EDGE) == ADC_ExternalTrigConvEdge_Falling) || \
                             ((EDGE) == ADC_ExternalTrigConvEdge_RisingFalling))
/**
  * @}
  */ 


/** @defgroup ADC_extrenal_trigger_sources_for_regular_channels_conversion 
  * @{
  */ 
#define ADC_ExternalTrigConv_T1_CC1                ((uint32_t)0x00000000)
#define ADC_ExternalTrigConv_T1_CC2                ((uint32_t)0x01000000)
#define ADC_ExternalTrigConv_T1_CC3                ((uint32_t)0x02000000)
#define ADC_ExternalTrigConv_T2_CC2                ((uint32_t)0x03000000)
#define ADC_ExternalTrigConv_T2_CC3                ((uint32_t)0x04000000)
#define ADC_ExternalTrigConv_T2_CC4                ((uint32_t)0x05000000)
#define ADC_ExternalTrigConv_T2_TRGO               ((uint32_t)0x06000000)
#define ADC_ExternalTrigConv_T3_CC1                ((uint32_t)0x07000000)
#define ADC_ExternalTrigConv_T3_TRGO               ((uint32_t)0x08000000)
#define ADC_ExternalTrigConv_T4_CC4                ((uint32_t)0x09000000)
#define ADC_ExternalTrigConv_T5_CC1                ((uint32_t)0x0A000000)
#define ADC_ExternalTrigConv_T5_CC2                ((uint32_t)0x0B000000)
#define ADC_ExternalTrigConv_T5_CC3                ((uint32_t)0x0C000000)
#define ADC_ExternalTrigConv_T8_CC1                ((uint32_t)0x0D000000)
#define ADC_ExternalTrigConv_T8_TRGO               ((uint32_t)0x0E000000)
#define ADC_ExternalTrigConv_Ext_IT11              ((uint32_t)0x0F000000)
#define IS_ADC_EXT_TRIG(REGTRIG) (((REGTRIG) == ADC_ExternalTrigConv_T1_CC1) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T1_CC2) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T1_CC3) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T2_CC2) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T2_CC3) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T2_CC4) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T2_TRGO) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T3_CC1) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T3_TRGO) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T4_CC4) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T5_CC1) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T5_CC2) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T5_CC3) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T8_CC1) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_T8_TRGO) || \
                                  ((REGTRIG) == ADC_ExternalTrigConv_Ext_IT11))
/**
  * @}
  */ 


/** @defgroup ADC_data_align 
  * @{
  */ 
#define ADC_DataAlign_Right                        ((uint32_t)0x00000000)
#define ADC_DataAlign_Left                         ((uint32_t)0x00000800)
#define IS_ADC_DATA_ALIGN(ALIGN) (((ALIGN) == ADC_DataAlign_Right) || \
                                  ((ALIGN) == ADC_DataAlign_Left))
/**
  * @}
  */ 


/** @defgroup ADC_channels 
  * @{
  */ 
#define ADC_Channel_0                               ((uint8_t)0x00)
#define ADC_Channel_1                               ((uint8_t)0x01)
#define ADC_Channel_2                               ((uint8_t)0x02)
#define ADC_Channel_3                               ((uint8_t)0x03)
#define ADC_Channel_4                               ((uint8_t)0x04)
#define ADC_Channel_5                               ((uint8_t)0x05)
#define ADC_Channel_6                               ((uint8_t)0x06)
#define ADC_Channel_7                               ((uint8_t)0x07)
#define ADC_Channel_8                               ((uint8_t)0x08)
#define ADC_Channel_9                               ((uint8_t)0x09)
#define ADC_Channel_10                              ((uint8_t)0x0A)
#define ADC_Channel_11                              ((uint8_t)0x0B)
#define ADC_Channel_12                              ((uint8_t)0x0C)
#define ADC_Channel_13                              ((uint8_t)0x0D)
#define ADC_Channel_14                              ((uint8_t)0x0E)
#define ADC_Channel_15                              ((uint8_t)0x0F)
#define ADC_Channel_16                              ((uint8_t)0x10)
#define ADC_Channel_17                              ((uint8_t)0x11)
#define ADC_Channel_18                              ((uint8_t)0x12)

#define ADC_Channel_TempSensor                      ((uint8_t)ADC_Channel_16)
#define ADC_Channel_Vrefint                         ((uint8_t)ADC_Channel_17)
#define ADC_Channel_Vbat                            ((uint8_t)ADC_Channel_18)

#define IS_ADC_CHANNEL(CHANNEL) (((CHANNEL) == ADC_Channel_0) || \
                                 ((CHANNEL) == ADC_Channel_1) || \
                                 ((CHANNEL) == ADC_Channel_2) || \
                                 ((CHANNEL) == ADC_Channel_3) || \
                                 ((CHANNEL) == ADC_Channel_4) || \
                                 ((CHANNEL) == ADC_Channel_5) || \
                                 ((CHANNEL) == ADC_Channel_6) || \
                                 ((CHANNEL) == ADC_Channel_7) || \
                                 ((CHANNEL) == ADC_Channel_8) || \
                                 ((CHANNEL) == ADC_Channel_9) || \
                                 ((CHANNEL) == ADC_Channel_10) || \
                                 ((CHANNEL) == ADC_Channel_11) || \
                                 ((CHANNEL) == ADC_Channel_12) || \
                                 ((CHANNEL) == ADC_Channel_13) || \
                                 ((CHANNEL) == ADC_Channel_14) || \
                                 ((CHANNEL) == ADC_Channel_15) || \
                                 ((CHANNEL) == ADC_Channel_16) || \
                                 ((CHANNEL) == ADC_Channel_17) || \
                                 ((CHANNEL) == ADC_Channel_18))
/**
  * @}
  */ 


/** @defgroup ADC_sampling_times 
  * @{
  */ 
#define ADC_SampleTime_3Cycles                    ((uint8_t)0x00)
#define ADC_SampleTime_15Cycles                   ((uint8_t)0x01)
#define ADC_SampleTime_28Cycles                   ((uint8_t)0x02)
#define ADC_SampleTime_56Cycles                   ((uint8_t)0x03)
#define ADC_SampleTime_84Cycles                   ((uint8_t)0x04)
#define ADC_SampleTime_112Cycles                  ((uint8_t)0x05)
#define ADC_SampleTime_144Cycles                  ((uint8_t)0x06)
#define ADC_SampleTime_480Cycles                  ((uint8_t)0x07)
#define IS_ADC_SAMPLE_TIME(TIME) (((TIME) == ADC_SampleTime_3Cycles) || \
                                  ((TIME) == ADC_SampleTime_15Cycles) || \
                                  ((TIME) == ADC_SampleTime_28Cycles) || \
                                  ((TIME) == ADC_SampleTime_56Cycles) || \
                                  ((TIME) == ADC_SampleTime_84Cycles) || \
                                  ((TIME) == ADC_SampleTime_112Cycles) || \
                                  ((TIME) == ADC_SampleTime_144Cycles) || \
                                  ((TIME) == ADC_SampleTime_480Cycles))
/**
  * @}
  */ 


/** @defgroup ADC_external_trigger_edge_for_injected_channels_conversion 
  * @{
  */ 
#define ADC_ExternalTrigInjecConvEdge_None          ((uint32_t)0x00000000)
#define ADC_ExternalTrigInjecConvEdge_Rising        ((uint32_t)0x00100000)
#define ADC_ExternalTrigInjecConvEdge_Falling       ((uint32_t)0x00200000)
#define ADC_ExternalTrigInjecConvEdge_RisingFalling ((uint32_t)0x00300000)
#define IS_ADC_EXT_INJEC_TRIG_EDGE(EDGE) (((EDGE) == ADC_ExternalTrigInjecConvEdge_None) || \
                                          ((EDGE) == ADC_ExternalTrigInjecConvEdge_Rising) || \
                                          ((EDGE) == ADC_ExternalTrigInjecConvEdge_Falling) || \
                                          ((EDGE) == ADC_ExternalTrigInjecConvEdge_RisingFalling))
                                            
/**
  * @}
  */ 


/** @defgroup ADC_extrenal_trigger_sources_for_injected_channels_conversion 
  * @{
  */ 
#define ADC_ExternalTrigInjecConv_T1_CC4            ((uint32_t)0x00000000)
#define ADC_ExternalTrigInjecConv_T1_TRGO           ((uint32_t)0x00010000)
#define ADC_ExternalTrigInjecConv_T2_CC1            ((uint32_t)0x00020000)
#define ADC_ExternalTrigInjecConv_T2_TRGO           ((uint32_t)0x00030000)
#define ADC_ExternalTrigInjecConv_T3_CC2            ((uint32_t)0x00040000)
#define ADC_ExternalTrigInjecConv_T3_CC4            ((uint32_t)0x00050000)
#define ADC_ExternalTrigInjecConv_T4_CC1            ((uint32_t)0x00060000)
#define ADC_ExternalTrigInjecConv_T4_CC2            ((uint32_t)0x00070000)
#define ADC_ExternalTrigInjecConv_T4_CC3            ((uint32_t)0x00080000)
#define ADC_ExternalTrigInjecConv_T4_TRGO           ((uint32_t)0x00090000)
#define ADC_ExternalTrigInjecConv_T5_CC4            ((uint32_t)0x000A0000)
#define ADC_ExternalTrigInjecConv_T5_TRGO           ((uint32_t)0x000B0000)
#define ADC_ExternalTrigInjecConv_T8_CC2            ((uint32_t)0x000C0000)
#define ADC_ExternalTrigInjecConv_T8_CC3            ((uint32_t)0x000D0000)
#define ADC_ExternalTrigInjecConv_T8_CC4            ((uint32_t)0x000E0000)
#define ADC_ExternalTrigInjecConv_Ext_IT15          ((uint32_t)0x000F0000)
#define IS_ADC_EXT_INJEC_TRIG(INJTRIG) (((INJTRIG) == ADC_ExternalTrigInjecConv_T1_CC4) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T1_TRGO) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T2_CC1) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T2_TRGO) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T3_CC2) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T3_CC4) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T4_CC1) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T4_CC2) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T4_CC3) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T4_TRGO) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T5_CC4) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T5_TRGO) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T8_CC2) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T8_CC3) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_T8_CC4) || \
                                        ((INJTRIG) == ADC_ExternalTrigInjecConv_Ext_IT15))
/**
  * @}
  */ 


/** @defgroup ADC_injected_channel_selection 
  * @{
  */ 
#define ADC_InjectedChannel_1                       ((uint8_t)0x14)
#define ADC_InjectedChannel_2                       ((uint8_t)0x18)
#define ADC_InjectedChannel_3                       ((uint8_t)0x1C)
#define ADC_InjectedChannel_4                       ((uint8_t)0x20)
#define IS_ADC_INJECTED_CHANNEL(CHANNEL) (((CHANNEL) == ADC_InjectedChannel_1) || \
                                          ((CHANNEL) == ADC_InjectedChannel_2) || \
                                          ((CHANNEL) == ADC_InjectedChannel_3) || \
                                          ((CHANNEL) == ADC_InjectedChannel_4))
/**
  * @}
  */ 


/** @defgroup ADC_analog_watchdog_selection 
  * @{
  */ 
#define ADC_AnalogWatchdog_SingleRegEnable         ((uint32_t)0x00800200)
#define ADC_AnalogWatchdog_SingleInjecEnable       ((uint32_t)0x00400200)
#define ADC_AnalogWatchdog_SingleRegOrInjecEnable  ((uint32_t)0x00C00200)
#define ADC_AnalogWatchdog_AllRegEnable            ((uint32_t)0x00800000)
#define ADC_AnalogWatchdog_AllInjecEnable          ((uint32_t)0x00400000)
#define ADC_AnalogWatchdog_AllRegAllInjecEnable    ((uint32_t)0x00C00000)
#define ADC_AnalogWatchdog_None                    ((uint32_t)0x00000000)
#define IS_ADC_ANALOG_WATCHDOG(WATCHDOG) (((WATCHDOG) == ADC_AnalogWatchdog_SingleRegEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_SingleInjecEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_SingleRegOrInjecEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_AllRegEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_AllInjecEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_AllRegAllInjecEnable) || \
                                          ((WATCHDOG) == ADC_AnalogWatchdog_None))
/**
  * @}
  */ 


/** @defgroup ADC_interrupts_definition 
  * @{
  */ 
#define ADC_IT_EOC                                 ((uint16_t)0x0205)  
#define ADC_IT_AWD                                 ((uint16_t)0x0106)  
#define ADC_IT_JEOC                                ((uint16_t)0x0407)  
#define ADC_IT_OVR                                 ((uint16_t)0x201A)  
#define IS_ADC_IT(IT) (((IT) == ADC_IT_EOC) || ((IT) == ADC_IT_AWD) || \
                       ((IT) == ADC_IT_JEOC)|| ((IT) == ADC_IT_OVR)) 
/**
  * @}
  */ 


/** @defgroup ADC_flags_definition 
  * @{
  */ 
#define ADC_FLAG_AWD                               ((uint8_t)0x01)
#define ADC_FLAG_EOC                               ((uint8_t)0x02)
#define ADC_FLAG_JEOC                              ((uint8_t)0x04)
#define ADC_FLAG_JSTRT                             ((uint8_t)0x08)
#define ADC_FLAG_STRT                              ((uint8_t)0x10)
#define ADC_FLAG_OVR                               ((uint8_t)0x20)   
  
#define IS_ADC_CLEAR_FLAG(FLAG) ((((FLAG) & (uint8_t)0xC0) == 0x00) && ((FLAG) != 0x00))   
#define IS_ADC_GET_FLAG(FLAG) (((FLAG) == ADC_FLAG_AWD) || \
                               ((FLAG) == ADC_FLAG_EOC) || \
                               ((FLAG) == ADC_FLAG_JEOC) || \
                               ((FLAG)== ADC_FLAG_JSTRT) || \
                               ((FLAG) == ADC_FLAG_STRT) || \
                               ((FLAG)== ADC_FLAG_OVR))     
/**
  * @}
  */ 


/** @defgroup ADC_thresholds 
  * @{
  */ 
#define IS_ADC_THRESHOLD(THRESHOLD) ((THRESHOLD) <= 0xFFF)
/**
  * @}
  */ 


/** @defgroup ADC_injected_offset 
  * @{
  */ 
#define IS_ADC_OFFSET(OFFSET) ((OFFSET) <= 0xFFF)
/**
  * @}
  */ 


/** @defgroup ADC_injected_length 
  * @{
  */ 
#define IS_ADC_INJECTED_LENGTH(LENGTH) (((LENGTH) >= 0x1) && ((LENGTH) <= 0x4))
/**
  * @}
  */ 


/** @defgroup ADC_injected_rank 
  * @{
  */ 
#define IS_ADC_INJECTED_RANK(RANK) (((RANK) >= 0x1) && ((RANK) <= 0x4))
/**
  * @}
  */ 


/** @defgroup ADC_regular_length 
  * @{
  */ 
#define IS_ADC_REGULAR_LENGTH(LENGTH) (((LENGTH) >= 0x1) && ((LENGTH) <= 0x10))
/**
  * @}
  */ 


/** @defgroup ADC_regular_rank 
  * @{
  */ 
#define IS_ADC_REGULAR_RANK(RANK) (((RANK) >= 0x1) && ((RANK) <= 0x10))
/**
  * @}
  */ 


/** @defgroup ADC_regular_discontinuous_mode_number 
  * @{
  */ 
#define IS_ADC_REGULAR_DISC_NUMBER(NUMBER) (((NUMBER) >= 0x1) && ((NUMBER) <= 0x8))
/**
  * @}
  */ 


/**
  * @}
  */ 

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/  

/*  Function used to set the ADC configuration to the default reset state *****/  
void ADC_DeInit(void);

/* Initialization and Configuration functions *********************************/
void ADC_Init(ADC_TypeDef* ADCx, ADC_InitTypeDef* ADC_InitStruct);
void ADC_StructInit(ADC_InitTypeDef* ADC_InitStruct);
void ADC_CommonInit(ADC_CommonInitTypeDef* ADC_CommonInitStruct);
void ADC_CommonStructInit(ADC_CommonInitTypeDef* ADC_CommonInitStruct);
void ADC_Cmd(ADC_TypeDef* ADCx, FunctionalState NewState);

/* Analog Watchdog configuration functions ************************************/
void ADC_AnalogWatchdogCmd(ADC_TypeDef* ADCx, uint32_t ADC_AnalogWatchdog);
void ADC_AnalogWatchdogThresholdsConfig(ADC_TypeDef* ADCx, uint16_t HighThreshold,uint16_t LowThreshold);
void ADC_AnalogWatchdogSingleChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel);

/* Temperature Sensor, Vrefint and VBAT management functions ******************/
void ADC_TempSensorVrefintCmd(FunctionalState NewState);
void ADC_VBATCmd(FunctionalState NewState);

/* Regular Channels Configuration functions ***********************************/
void ADC_RegularChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel, uint8_t Rank, uint8_t ADC_SampleTime);
void ADC_SoftwareStartConv(ADC_TypeDef* ADCx);
FlagStatus ADC_GetSoftwareStartConvStatus(ADC_TypeDef* ADCx);
void ADC_EOCOnEachRegularChannelCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
void ADC_ContinuousModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
void ADC_DiscModeChannelCountConfig(ADC_TypeDef* ADCx, uint8_t Number);
void ADC_DiscModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
uint16_t ADC_GetConversionValue(ADC_TypeDef* ADCx);
uint32_t ADC_GetMultiModeConversionValue(void);

/* Regular Channels DMA Configuration functions *******************************/
void ADC_DMACmd(ADC_TypeDef* ADCx, FunctionalState NewState);
void ADC_DMARequestAfterLastTransferCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
void ADC_MultiModeDMARequestAfterLastTransferCmd(FunctionalState NewState);

/* Injected channels Configuration functions **********************************/
void ADC_InjectedChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel, uint8_t Rank, uint8_t ADC_SampleTime);
void ADC_InjectedSequencerLengthConfig(ADC_TypeDef* ADCx, uint8_t Length);
void ADC_SetInjectedOffset(ADC_TypeDef* ADCx, uint8_t ADC_InjectedChannel, uint16_t Offset);
void ADC_ExternalTrigInjectedConvConfig(ADC_TypeDef* ADCx, uint32_t ADC_ExternalTrigInjecConv);
void ADC_ExternalTrigInjectedConvEdgeConfig(ADC_TypeDef* ADCx, uint32_t ADC_ExternalTrigInjecConvEdge);
void ADC_SoftwareStartInjectedConv(ADC_TypeDef* ADCx);
FlagStatus ADC_GetSoftwareStartInjectedConvCmdStatus(ADC_TypeDef* ADCx);
void ADC_AutoInjectedConvCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
void ADC_InjectedDiscModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState);
uint16_t ADC_GetInjectedConversionValue(ADC_TypeDef* ADCx, uint8_t ADC_InjectedChannel);

/* Interrupts and flags management functions **********************************/
void ADC_ITConfig(ADC_TypeDef* ADCx, uint16_t ADC_IT, FunctionalState NewState);
FlagStatus ADC_GetFlagStatus(ADC_TypeDef* ADCx, uint8_t ADC_FLAG);
void ADC_ClearFlag(ADC_TypeDef* ADCx, uint8_t ADC_FLAG);
ITStatus ADC_GetITStatus(ADC_TypeDef* ADCx, uint16_t ADC_IT);
void ADC_ClearITPendingBit(ADC_TypeDef* ADCx, uint16_t ADC_IT);

#ifdef __cplusplus
}
#endif

#endif /*__STM32F4xx_ADC_H */

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

/**
  ******************************************************************************
  * @file    stm32f4xx_dac.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the DAC firmware 
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
#ifndef __STM32F4xx_DAC_H
#define __STM32F4xx_DAC_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup DAC
  * @{
  */

/* Exported types ------------------------------------------------------------*/

/** 
  * @brief  DAC Init structure definition
  */

typedef struct
{
  uint32_t DAC_Trigger;                      /*!< Specifies the external trigger for the selected DAC channel.
                                                  This parameter can be a value of @ref DAC_trigger_selection */

  uint32_t DAC_WaveGeneration;               /*!< Specifies whether DAC channel noise waves or triangle waves
                                                  are generated, or whether no wave is generated.
                                                  This parameter can be a value of @ref DAC_wave_generation */

  uint32_t DAC_LFSRUnmask_TriangleAmplitude; /*!< Specifies the LFSR mask for noise wave generation or
                                                  the maximum amplitude triangle generation for the DAC channel. 
                                                  This parameter can be a value of @ref DAC_lfsrunmask_triangleamplitude */

  uint32_t DAC_OutputBuffer;                 /*!< Specifies whether the DAC channel output buffer is enabled or disabled.
                                                  This parameter can be a value of @ref DAC_output_buffer */
}DAC_InitTypeDef;

/* Exported constants --------------------------------------------------------*/

/** @defgroup DAC_Exported_Constants
  * @{
  */

/** @defgroup DAC_trigger_selection 
  * @{
  */

#define DAC_Trigger_None                   ((uint32_t)0x00000000) /*!< Conversion is automatic once the DAC1_DHRxxxx register 
                                                                       has been loaded, and not by external trigger */
#define DAC_Trigger_T2_TRGO                ((uint32_t)0x00000024) /*!< TIM2 TRGO selected as external conversion trigger for DAC channel */
#define DAC_Trigger_T4_TRGO                ((uint32_t)0x0000002C) /*!< TIM4 TRGO selected as external conversion trigger for DAC channel */
#define DAC_Trigger_T5_TRGO                ((uint32_t)0x0000001C) /*!< TIM5 TRGO selected as external conversion trigger for DAC channel */
#define DAC_Trigger_T6_TRGO                ((uint32_t)0x00000004) /*!< TIM6 TRGO selected as external conversion trigger for DAC channel */
#define DAC_Trigger_T7_TRGO                ((uint32_t)0x00000014) /*!< TIM7 TRGO selected as external conversion trigger for DAC channel */
#define DAC_Trigger_T8_TRGO                ((uint32_t)0x0000000C) /*!< TIM8 TRGO selected as external conversion trigger for DAC channel */                                                                       

#define DAC_Trigger_Ext_IT9                ((uint32_t)0x00000034) /*!< EXTI Line9 event selected as external conversion trigger for DAC channel */
#define DAC_Trigger_Software               ((uint32_t)0x0000003C) /*!< Conversion started by software trigger for DAC channel */

#define IS_DAC_TRIGGER(TRIGGER) (((TRIGGER) == DAC_Trigger_None) || \
                                 ((TRIGGER) == DAC_Trigger_T6_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_T8_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_T7_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_T5_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_T2_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_T4_TRGO) || \
                                 ((TRIGGER) == DAC_Trigger_Ext_IT9) || \
                                 ((TRIGGER) == DAC_Trigger_Software))

/**
  * @}
  */

/** @defgroup DAC_wave_generation 
  * @{
  */

#define DAC_WaveGeneration_None            ((uint32_t)0x00000000)
#define DAC_WaveGeneration_Noise           ((uint32_t)0x00000040)
#define DAC_WaveGeneration_Triangle        ((uint32_t)0x00000080)
#define IS_DAC_GENERATE_WAVE(WAVE) (((WAVE) == DAC_WaveGeneration_None) || \
                                    ((WAVE) == DAC_WaveGeneration_Noise) || \
                                    ((WAVE) == DAC_WaveGeneration_Triangle))
/**
  * @}
  */

/** @defgroup DAC_lfsrunmask_triangleamplitude
  * @{
  */

#define DAC_LFSRUnmask_Bit0                ((uint32_t)0x00000000) /*!< Unmask DAC channel LFSR bit0 for noise wave generation */
#define DAC_LFSRUnmask_Bits1_0             ((uint32_t)0x00000100) /*!< Unmask DAC channel LFSR bit[1:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits2_0             ((uint32_t)0x00000200) /*!< Unmask DAC channel LFSR bit[2:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits3_0             ((uint32_t)0x00000300) /*!< Unmask DAC channel LFSR bit[3:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits4_0             ((uint32_t)0x00000400) /*!< Unmask DAC channel LFSR bit[4:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits5_0             ((uint32_t)0x00000500) /*!< Unmask DAC channel LFSR bit[5:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits6_0             ((uint32_t)0x00000600) /*!< Unmask DAC channel LFSR bit[6:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits7_0             ((uint32_t)0x00000700) /*!< Unmask DAC channel LFSR bit[7:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits8_0             ((uint32_t)0x00000800) /*!< Unmask DAC channel LFSR bit[8:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits9_0             ((uint32_t)0x00000900) /*!< Unmask DAC channel LFSR bit[9:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits10_0            ((uint32_t)0x00000A00) /*!< Unmask DAC channel LFSR bit[10:0] for noise wave generation */
#define DAC_LFSRUnmask_Bits11_0            ((uint32_t)0x00000B00) /*!< Unmask DAC channel LFSR bit[11:0] for noise wave generation */
#define DAC_TriangleAmplitude_1            ((uint32_t)0x00000000) /*!< Select max triangle amplitude of 1 */
#define DAC_TriangleAmplitude_3            ((uint32_t)0x00000100) /*!< Select max triangle amplitude of 3 */
#define DAC_TriangleAmplitude_7            ((uint32_t)0x00000200) /*!< Select max triangle amplitude of 7 */
#define DAC_TriangleAmplitude_15           ((uint32_t)0x00000300) /*!< Select max triangle amplitude of 15 */
#define DAC_TriangleAmplitude_31           ((uint32_t)0x00000400) /*!< Select max triangle amplitude of 31 */
#define DAC_TriangleAmplitude_63           ((uint32_t)0x00000500) /*!< Select max triangle amplitude of 63 */
#define DAC_TriangleAmplitude_127          ((uint32_t)0x00000600) /*!< Select max triangle amplitude of 127 */
#define DAC_TriangleAmplitude_255          ((uint32_t)0x00000700) /*!< Select max triangle amplitude of 255 */
#define DAC_TriangleAmplitude_511          ((uint32_t)0x00000800) /*!< Select max triangle amplitude of 511 */
#define DAC_TriangleAmplitude_1023         ((uint32_t)0x00000900) /*!< Select max triangle amplitude of 1023 */
#define DAC_TriangleAmplitude_2047         ((uint32_t)0x00000A00) /*!< Select max triangle amplitude of 2047 */
#define DAC_TriangleAmplitude_4095         ((uint32_t)0x00000B00) /*!< Select max triangle amplitude of 4095 */

#define IS_DAC_LFSR_UNMASK_TRIANGLE_AMPLITUDE(VALUE) (((VALUE) == DAC_LFSRUnmask_Bit0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits1_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits2_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits3_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits4_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits5_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits6_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits7_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits8_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits9_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits10_0) || \
                                                      ((VALUE) == DAC_LFSRUnmask_Bits11_0) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_1) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_3) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_7) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_15) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_31) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_63) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_127) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_255) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_511) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_1023) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_2047) || \
                                                      ((VALUE) == DAC_TriangleAmplitude_4095))
/**
  * @}
  */

/** @defgroup DAC_output_buffer 
  * @{
  */

#define DAC_OutputBuffer_Enable            ((uint32_t)0x00000000)
#define DAC_OutputBuffer_Disable           ((uint32_t)0x00000002)
#define IS_DAC_OUTPUT_BUFFER_STATE(STATE) (((STATE) == DAC_OutputBuffer_Enable) || \
                                           ((STATE) == DAC_OutputBuffer_Disable))
/**
  * @}
  */

/** @defgroup DAC_Channel_selection 
  * @{
  */

#define DAC_Channel_1                      ((uint32_t)0x00000000)
#define DAC_Channel_2                      ((uint32_t)0x00000010)
#define IS_DAC_CHANNEL(CHANNEL) (((CHANNEL) == DAC_Channel_1) || \
                                 ((CHANNEL) == DAC_Channel_2))
/**
  * @}
  */

/** @defgroup DAC_data_alignement 
  * @{
  */

#define DAC_Align_12b_R                    ((uint32_t)0x00000000)
#define DAC_Align_12b_L                    ((uint32_t)0x00000004)
#define DAC_Align_8b_R                     ((uint32_t)0x00000008)
#define IS_DAC_ALIGN(ALIGN) (((ALIGN) == DAC_Align_12b_R) || \
                             ((ALIGN) == DAC_Align_12b_L) || \
                             ((ALIGN) == DAC_Align_8b_R))
/**
  * @}
  */

/** @defgroup DAC_wave_generation 
  * @{
  */

#define DAC_Wave_Noise                     ((uint32_t)0x00000040)
#define DAC_Wave_Triangle                  ((uint32_t)0x00000080)
#define IS_DAC_WAVE(WAVE) (((WAVE) == DAC_Wave_Noise) || \
                           ((WAVE) == DAC_Wave_Triangle))
/**
  * @}
  */

/** @defgroup DAC_data 
  * @{
  */

#define IS_DAC_DATA(DATA) ((DATA) <= 0xFFF0) 
/**
  * @}
  */
  
/** @defgroup DAC_interrupts_definition 
  * @{
  */   
#define DAC_IT_DMAUDR                      ((uint32_t)0x00002000)  
#define IS_DAC_IT(IT) (((IT) == DAC_IT_DMAUDR)) 

/**
  * @}
  */ 

/** @defgroup DAC_flags_definition 
  * @{
  */ 
  
#define DAC_FLAG_DMAUDR                    ((uint32_t)0x00002000)  
#define IS_DAC_FLAG(FLAG) (((FLAG) == DAC_FLAG_DMAUDR))  

/**
  * @}
  */

/**
  * @}
  */

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/  

/*  Function used to set the DAC configuration to the default reset state *****/  
void DAC_DeInit(void);

/*  DAC channels configuration: trigger, output buffer, data format functions */
void DAC_Init(uint32_t DAC_Channel, DAC_InitTypeDef* DAC_InitStruct);
void DAC_StructInit(DAC_InitTypeDef* DAC_InitStruct);
void DAC_Cmd(uint32_t DAC_Channel, FunctionalState NewState);
void DAC_SoftwareTriggerCmd(uint32_t DAC_Channel, FunctionalState NewState);
void DAC_DualSoftwareTriggerCmd(FunctionalState NewState);
void DAC_WaveGenerationCmd(uint32_t DAC_Channel, uint32_t DAC_Wave, FunctionalState NewState);
void DAC_SetChannel1Data(uint32_t DAC_Align, uint16_t Data);
void DAC_SetChannel2Data(uint32_t DAC_Align, uint16_t Data);
void DAC_SetDualChannelData(uint32_t DAC_Align, uint16_t Data2, uint16_t Data1);
uint16_t DAC_GetDataOutputValue(uint32_t DAC_Channel);

/* DMA management functions ***************************************************/
void DAC_DMACmd(uint32_t DAC_Channel, FunctionalState NewState);

/* Interrupts and flags management functions **********************************/
void DAC_ITConfig(uint32_t DAC_Channel, uint32_t DAC_IT, FunctionalState NewState);
FlagStatus DAC_GetFlagStatus(uint32_t DAC_Channel, uint32_t DAC_FLAG);
void DAC_ClearFlag(uint32_t DAC_Channel, uint32_t DAC_FLAG);
ITStatus DAC_GetITStatus(uint32_t DAC_Channel, uint32_t DAC_IT);
void DAC_ClearITPendingBit(uint32_t DAC_Channel, uint32_t DAC_IT);

#ifdef __cplusplus
}
#endif

#endif /*__STM32F4xx_DAC_H */

/**
  * @}
  */

/**
  * @}
  */

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/

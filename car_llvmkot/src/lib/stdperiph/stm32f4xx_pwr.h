/**
  ******************************************************************************
  * @file    stm32f4xx_pwr.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the PWR firmware 
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
#ifndef __STM32F4xx_PWR_H
#define __STM32F4xx_PWR_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup PWR
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/
/* Exported constants --------------------------------------------------------*/

/** @defgroup PWR_Exported_Constants
  * @{
  */ 

/** @defgroup PWR_PVD_detection_level 
  * @{
  */ 

#define PWR_PVDLevel_0                  PWR_CR_PLS_LEV0
#define PWR_PVDLevel_1                  PWR_CR_PLS_LEV1
#define PWR_PVDLevel_2                  PWR_CR_PLS_LEV2
#define PWR_PVDLevel_3                  PWR_CR_PLS_LEV3
#define PWR_PVDLevel_4                  PWR_CR_PLS_LEV4
#define PWR_PVDLevel_5                  PWR_CR_PLS_LEV5
#define PWR_PVDLevel_6                  PWR_CR_PLS_LEV6
#define PWR_PVDLevel_7                  PWR_CR_PLS_LEV7

#define IS_PWR_PVD_LEVEL(LEVEL) (((LEVEL) == PWR_PVDLevel_0) || ((LEVEL) == PWR_PVDLevel_1)|| \
                                 ((LEVEL) == PWR_PVDLevel_2) || ((LEVEL) == PWR_PVDLevel_3)|| \
                                 ((LEVEL) == PWR_PVDLevel_4) || ((LEVEL) == PWR_PVDLevel_5)|| \
                                 ((LEVEL) == PWR_PVDLevel_6) || ((LEVEL) == PWR_PVDLevel_7))
/**
  * @}
  */

  
/** @defgroup PWR_Regulator_state_in_STOP_mode 
  * @{
  */

#define PWR_Regulator_ON                ((uint32_t)0x00000000)
#define PWR_Regulator_LowPower          PWR_CR_LPDS
#define IS_PWR_REGULATOR(REGULATOR) (((REGULATOR) == PWR_Regulator_ON) || \
                                     ((REGULATOR) == PWR_Regulator_LowPower))
/**
  * @}
  */

/** @defgroup PWR_STOP_mode_entry 
  * @{
  */

#define PWR_STOPEntry_WFI               ((uint8_t)0x01)
#define PWR_STOPEntry_WFE               ((uint8_t)0x02)
#define IS_PWR_STOP_ENTRY(ENTRY) (((ENTRY) == PWR_STOPEntry_WFI) || ((ENTRY) == PWR_STOPEntry_WFE))

/** @defgroup PWR_Regulator_Voltage_Scale 
  * @{
  */

#define PWR_Regulator_Voltage_Scale1    ((uint32_t)0x00004000)
#define PWR_Regulator_Voltage_Scale2    ((uint32_t)0x00000000)
#define IS_PWR_REGULATOR_VOLTAGE(VOLTAGE) (((VOLTAGE) == PWR_Regulator_Voltage_Scale1) || ((VOLTAGE) == PWR_Regulator_Voltage_Scale2))
 
/**
  * @}
  */

/** @defgroup PWR_Flag 
  * @{
  */

#define PWR_FLAG_WU                     PWR_CSR_WUF
#define PWR_FLAG_SB                     PWR_CSR_SBF
#define PWR_FLAG_PVDO                   PWR_CSR_PVDO
#define PWR_FLAG_BRR                    PWR_CSR_BRR
#define PWR_FLAG_VOSRDY                 PWR_CSR_VOSRDY

/** @defgroup PWR_Flag_Legacy 
  * @{
  */
#define PWR_FLAG_REGRDY                  PWR_FLAG_VOSRDY               
/**
  * @}
  */

#define IS_PWR_GET_FLAG(FLAG) (((FLAG) == PWR_FLAG_WU) || ((FLAG) == PWR_FLAG_SB) || \
                               ((FLAG) == PWR_FLAG_PVDO) || ((FLAG) == PWR_FLAG_BRR) || \
                               ((FLAG) == PWR_FLAG_VOSRDY))

#define IS_PWR_CLEAR_FLAG(FLAG) (((FLAG) == PWR_FLAG_WU) || ((FLAG) == PWR_FLAG_SB))
/**
  * @}
  */

/**
  * @}
  */

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/ 

/* Function used to set the PWR configuration to the default reset state ******/ 
void PWR_DeInit(void);

/* Backup Domain Access function **********************************************/ 
void PWR_BackupAccessCmd(FunctionalState NewState);

/* PVD configuration functions ************************************************/ 
void PWR_PVDLevelConfig(uint32_t PWR_PVDLevel);
void PWR_PVDCmd(FunctionalState NewState);

/* WakeUp pins configuration functions ****************************************/ 
void PWR_WakeUpPinCmd(FunctionalState NewState);

/* Main and Backup Regulators configuration functions *************************/ 
void PWR_BackupRegulatorCmd(FunctionalState NewState);
void PWR_MainRegulatorModeConfig(uint32_t PWR_Regulator_Voltage);

/* FLASH Power Down configuration functions ***********************************/ 
void PWR_FlashPowerDownCmd(FunctionalState NewState);

/* Low Power modes configuration functions ************************************/ 
void PWR_EnterSTOPMode(uint32_t PWR_Regulator, uint8_t PWR_STOPEntry);
void PWR_EnterSTANDBYMode(void);

/* Flags management functions *************************************************/ 
FlagStatus PWR_GetFlagStatus(uint32_t PWR_FLAG);
void PWR_ClearFlag(uint32_t PWR_FLAG);

#ifdef __cplusplus
}
#endif

#endif /* __STM32F4xx_PWR_H */

/**
  * @}
  */

/**
  * @}
  */

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
